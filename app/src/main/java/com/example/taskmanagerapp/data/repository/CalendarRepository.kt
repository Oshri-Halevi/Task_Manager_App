package com.example.taskmanagerapp.data.repository

import com.example.taskmanagerapp.data.calendar.CalendarDataSource
import com.example.taskmanagerapp.data.calendar.GoogleCalendarDataSource
import com.example.taskmanagerapp.data.local.CalendarImportMapDao
import com.example.taskmanagerapp.data.local.CalendarImportMapEntity
import com.example.taskmanagerapp.data.local.SyncState
import com.example.taskmanagerapp.data.local.Task

/**
 * Result of calendar import operation.
 */
data class CalendarImportResult(
    val imported: Int,
    val skipped: Int,
    val listId: Long,
    val totalEvents: Int
)

data class CalendarSyncResult(
    val created: Int,
    val updated: Int,
    val skipped: Int
)

/**
 * Repository for calendar import operations.
 * Handles external calendar integration and import mapping.
 * Task persistence is delegated to TaskRepository to keep a single task source of truth.
 */
class CalendarRepository(
    private val calendarDataSource: CalendarDataSource,
    private val calendarImportMapDao: CalendarImportMapDao,
    private val listRepository: ListRepository,
    private val taskRepository: TaskRepository,
    private val importedListName: String
) {
    /**
     * Import calendar events as tasks.
     * Creates an "Imported" list if not exists.
     * Skips events that have already been imported.
     *
     * @return CalendarImportResult with counts of imported and skipped events
     */
    suspend fun importCalendar(): CalendarImportResult {
        // Get or create "Imported" list
        val importedListId = getOrCreateImportedList()

        // Fetch events from calendar
        val events = calendarDataSource.fetchEvents(days = 30)

        // Remove stale mappings where task no longer exists
        calendarImportMapDao.deleteOrphans()

        // Get already imported event IDs
        val existingEventIds = calendarImportMapDao.getAllImportedEventIds().toSet()

        var imported = 0
        var skipped = 0

        for (event in events) {
            if (existingEventIds.contains(event.eventId)) {
                skipped++
                continue
            }

            // Create task from event
            val task = Task(
                title = event.title,
                description = event.description,
                isDone = false,
                priority = 1, // Normal priority
                dueDate = event.startTime,
                listId = importedListId,
                // Imported tasks should be pushed on next sync.
                syncState = SyncState.DIRTY,
                updatedAt = System.currentTimeMillis()
            )

            // Insert task and get its ID
            val insertedId = taskRepository.insertImportedTask(task)

            // Record the import mapping
            calendarImportMapDao.insert(
                CalendarImportMapEntity(
                    eventId = event.eventId,
                    taskId = insertedId
                )
            )
            imported++
        }

        return CalendarImportResult(
            imported = imported,
            skipped = skipped,
            listId = importedListId,
            totalEvents = events.size
        )
    }

    suspend fun clearImportHistory() {
        calendarImportMapDao.deleteAll()
    }

    fun requiredImportScopes(): List<String> = calendarDataSource.importScopes()

    fun requiredSyncScopes(): List<String> = calendarDataSource.syncScopes()

    suspend fun syncTasksToCalendar(): CalendarSyncResult {
        val googleDataSource = calendarDataSource as? GoogleCalendarDataSource
            ?: return CalendarSyncResult(created = 0, updated = 0, skipped = 0)

        val tasks = taskRepository.getAllActiveTasksOnce()
        if (tasks.isEmpty()) {
            return CalendarSyncResult(created = 0, updated = 0, skipped = 0)
        }

        val existingRefs = calendarImportMapDao.getAllMappings()
            .groupBy { it.taskId }
            .mapValues { (_, mappings) -> mappings.maxByOrNull { it.importedAt }!!.eventId }

        val syncResult = googleDataSource.syncTasksToCalendar(tasks, existingRefs)
        syncResult.taskEventRefs.forEach { (taskId, eventRef) ->
            calendarImportMapDao.deleteByTaskId(taskId)
            calendarImportMapDao.insert(
                CalendarImportMapEntity(
                    eventId = eventRef,
                    taskId = taskId
                )
            )
        }

        return CalendarSyncResult(
            created = syncResult.created,
            updated = syncResult.updated,
            skipped = syncResult.skipped
        )
    }

    private suspend fun getOrCreateImportedList(): Long {
        val existingLists = listRepository.getListsSnapshot()
        val importedList = existingLists.find { it.name == importedListName }
        
        if (importedList != null) {
            return importedList.id
        }

        // Create new "Imported" list
        listRepository.createList(importedListName)

        // Find the newly created list
        return listRepository.getListsSnapshot().find { it.name == importedListName }?.id ?: 1L
    }
}
