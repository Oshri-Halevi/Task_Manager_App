package com.example.taskmanagerapp.data.calendar

import android.content.Context
import android.util.Log
import com.example.taskmanagerapp.data.local.Task
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Calendar as JavaCalendar
import java.util.Locale

data class CalendarTaskSyncResult(
    val created: Int,
    val updated: Int,
    val skipped: Int,
    val taskEventRefs: Map<Int, String>
)

/**
 * Real Google Calendar data source.
 * Reads events + Google Tasks and syncs local tasks to Google Calendar events.
 */
class GoogleCalendarDataSource(private val context: Context) : CalendarDataSource {

    companion object {
        private const val TAG = "GoogleCalendar"
        private const val APP_NAME = "TaskManagerApp"
        private const val PRIMARY_CALENDAR = "primary"
        private const val TASKS_API_BASE = "https://tasks.googleapis.com/tasks/v1"
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        const val CALENDAR_SCOPE = CalendarScopes.CALENDAR
        const val TASKS_READONLY_SCOPE = "https://www.googleapis.com/auth/tasks.readonly"
        val IMPORT_SCOPES: List<String> = listOf(CALENDAR_SCOPE, TASKS_READONLY_SCOPE)
        val SYNC_SCOPES: List<String> = listOf(CALENDAR_SCOPE)
    }

    override suspend fun fetchEvents(days: Int): List<CalendarEvent> {
        val account = requireSignedInAccount(IMPORT_SCOPES)

        return withContext(Dispatchers.IO) {
            val service = createCalendarService(account, listOf(CALENDAR_SCOPE))

            val windowStart = JavaCalendar.getInstance().apply {
                set(JavaCalendar.HOUR_OF_DAY, 0)
                set(JavaCalendar.MINUTE, 0)
                set(JavaCalendar.SECOND, 0)
                set(JavaCalendar.MILLISECOND, 0)
            }.timeInMillis
            val windowEnd = windowStart + days.toLong() * 24 * 60 * 60 * 1000

            Log.d(TAG, "Fetching calendar events for next $days days...")

            val calendarIds = runCatching {
                service.calendarList().list()
                    .setMinAccessRole("reader")
                    .execute()
                    .items
                    ?.mapNotNull { it.id }
                    ?.ifEmpty { listOf("primary") }
                    ?: listOf("primary")
            }.getOrElse {
                Log.w(TAG, "Failed to load calendar list, falling back to primary", it)
                listOf("primary")
            }

            val allEvents = mutableListOf<CalendarEvent>()
            calendarIds.forEach { calendarId ->
                val events = service.events().list(calendarId)
                    .setTimeMin(DateTime(windowStart))
                    .setTimeMax(DateTime(windowEnd))
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .setShowDeleted(false)
                    .setMaxResults(250)
                    .execute()

                val items = events.items ?: emptyList()
                Log.d(TAG, "Fetched ${items.size} events from calendar '$calendarId'")
                items.forEach { event ->
                    val rawEventId = event.id ?: return@forEach
                    val startMillis = event.start?.dateTime?.value
                        ?: event.start?.date?.value
                        ?: windowStart
                    val endMillis = event.end?.dateTime?.value
                        ?: event.end?.date?.value
                        ?: startMillis

                    allEvents += CalendarEvent(
                        eventId = "$calendarId:$rawEventId",
                        title = event.summary ?: "Untitled Event",
                        description = event.description ?: "",
                        startTime = startMillis,
                        endTime = endMillis
                    )
                }
            }

            val taskEvents = fetchGoogleTasksAsEvents(account, windowStart, windowEnd)
            allEvents += taskEvents

            allEvents
                .distinctBy { it.eventId }
                .sortedBy { it.startTime }
        }
    }

    override fun importScopes(): List<String> = IMPORT_SCOPES

    override fun syncScopes(): List<String> = SYNC_SCOPES

    suspend fun syncTasksToCalendar(
        tasks: List<Task>,
        existingRefsByTaskId: Map<Int, String>
    ): CalendarTaskSyncResult {
        val account = requireSignedInAccount(SYNC_SCOPES)
        return withContext(Dispatchers.IO) {
            val service = createCalendarService(account, listOf(CALENDAR_SCOPE))
            val newRefs = mutableMapOf<Int, String>()
            var created = 0
            var updated = 0
            var skipped = 0

            tasks.forEach { task ->
                if (task.title.isBlank()) {
                    skipped++
                    return@forEach
                }
                val existingRef = existingRefsByTaskId[task.id]
                if (existingRef != null && existingRef.startsWith("gtask:")) {
                    // Google Tasks imports are not Google Calendar events; skip pushing these.
                    skipped++
                    return@forEach
                }

                val payload = task.toCalendarEvent()

                if (existingRef != null) {
                    val parsed = parseEventRef(existingRef)
                    if (parsed != null) {
                        try {
                            val updatedEvent = service.events()
                                .update(parsed.calendarId, parsed.eventId, payload)
                                .execute()
                            val updatedId = updatedEvent.id ?: parsed.eventId
                            newRefs[task.id] = toEventRef(parsed.calendarId, updatedId)
                            updated++
                            return@forEach
                        } catch (e: GoogleJsonResponseException) {
                            if (e.statusCode != 404 && e.statusCode != 410) throw e
                            Log.w(TAG, "Mapped event missing, recreating for task ${task.id}", e)
                        }
                    }
                }

                val insertedEvent = service.events()
                    .insert(PRIMARY_CALENDAR, payload)
                    .execute()
                val insertedId = insertedEvent.id
                if (insertedId.isNullOrBlank()) {
                    skipped++
                } else {
                    newRefs[task.id] = toEventRef(PRIMARY_CALENDAR, insertedId)
                    created++
                }
            }

            CalendarTaskSyncResult(
                created = created,
                updated = updated,
                skipped = skipped,
                taskEventRefs = newRefs
            )
        }
    }

    private fun requireSignedInAccount(requiredScopes: List<String>) =
        GoogleSignIn.getLastSignedInAccount(context)
            ?.takeIf { account ->
                GoogleSignIn.hasPermissions(
                    account,
                    *requiredScopes.map(::Scope).toTypedArray()
                )
            }
            ?: throw IllegalStateException("Google account or required permissions are missing")

    private fun createCalendarService(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        scopes: List<String>
    ): Calendar {
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = account.account
        return Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }

    private fun fetchGoogleTasksAsEvents(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        windowStart: Long,
        windowEnd: Long
    ): List<CalendarEvent> {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(TASKS_READONLY_SCOPE))
        credential.selectedAccount = account.account
        val token = credential.token
            ?: throw IllegalStateException("Unable to get Google Tasks access token")

        val listsJson = getJson("$TASKS_API_BASE/users/@me/lists?maxResults=100", token)
        val listItems = listsJson.optJSONArray("items") ?: return emptyList()
        val out = mutableListOf<CalendarEvent>()

        for (i in 0 until listItems.length()) {
            val listObj = listItems.optJSONObject(i) ?: continue
            val listId = listObj.optString("id")
            if (listId.isBlank()) continue
            val listTitle = listObj.optString("title", "Google Tasks")
            val encodedListId = URLEncoder.encode(listId, "UTF-8")

            val tasksJson = getJson(
                "$TASKS_API_BASE/lists/$encodedListId/tasks?showCompleted=false&showDeleted=false&showHidden=false&maxResults=100",
                token
            )
            val taskItems = tasksJson.optJSONArray("items") ?: continue
            for (j in 0 until taskItems.length()) {
                val taskObj = taskItems.optJSONObject(j) ?: continue
                if (taskObj.optString("status") == "completed") continue

                val taskId = taskObj.optString("id")
                if (taskId.isBlank()) continue

                val dueIso = taskObj.optString("due").takeIf { it.isNotBlank() }
                val dueMillis = dueIso?.let { runCatching { DateTime.parseRfc3339(it).value }.getOrNull() }
                val startMillis = dueMillis ?: windowStart
                if (dueMillis != null && (startMillis < windowStart || startMillis > windowEnd)) {
                    continue
                }
                val endMillis = startMillis + 30L * 60L * 1000L
                val notes = taskObj.optString("notes").takeIf { it.isNotBlank() } ?: ""
                val enrichedDescription = buildString {
                    if (notes.isNotBlank()) append(notes).append("\n\n")
                    append("From Google Tasks: ").append(listTitle)
                }

                out += CalendarEvent(
                    eventId = "gtask:$listId:$taskId",
                    title = taskObj.optString("title", "Untitled Task"),
                    description = enrichedDescription,
                    startTime = startMillis,
                    endTime = endMillis
                )
            }
        }

        Log.d(TAG, "Fetched ${out.size} items from Google Tasks")
        return out
    }

    private fun getJson(url: String, accessToken: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val responseCode = connection.responseCode
            val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (responseCode !in 200..299) {
                throw IllegalStateException("Google Tasks API error $responseCode: $body")
            }
            JSONObject(if (body.isBlank()) "{}" else body)
        } finally {
            connection.disconnect()
        }
    }

    private fun Task.toCalendarEvent(): Event {
        val startDay = JavaCalendar.getInstance().apply {
            timeInMillis = dueDate ?: System.currentTimeMillis()
            set(JavaCalendar.HOUR_OF_DAY, 0)
            set(JavaCalendar.MINUTE, 0)
            set(JavaCalendar.SECOND, 0)
            set(JavaCalendar.MILLISECOND, 0)
        }
        val endDay = (startDay.clone() as JavaCalendar).apply {
            add(JavaCalendar.DAY_OF_MONTH, 1)
        }
        val descriptionText = buildString {
            if (description.isNotBlank()) append(description).append("\n\n")
            append("Synced from TaskManagerApp (taskId=").append(id).append(")")
        }
        return Event()
            .setSummary(title)
            .setDescription(descriptionText)
            // Use date-only ISO values to avoid timezone shift (e.g., 18 -> 17).
            .setStart(EventDateTime().setDate(DateTime.parseRfc3339(toIsoDate(startDay))))
            .setEnd(EventDateTime().setDate(DateTime.parseRfc3339(toIsoDate(endDay))))
    }

    private fun parseEventRef(ref: String): EventRef? {
        val split = ref.indexOf(':')
        if (split <= 0 || split == ref.lastIndex) return null
        val calendarId = ref.substring(0, split)
        if (calendarId == "gtask") return null
        val eventId = ref.substring(split + 1)
        if (eventId.isBlank()) return null
        return EventRef(calendarId, eventId)
    }

    private fun toEventRef(calendarId: String, eventId: String): String = "$calendarId:$eventId"

    private fun toIsoDate(calendar: JavaCalendar): String =
        String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(JavaCalendar.YEAR),
            calendar.get(JavaCalendar.MONTH) + 1,
            calendar.get(JavaCalendar.DAY_OF_MONTH)
        )

    private data class EventRef(val calendarId: String, val eventId: String)
}
