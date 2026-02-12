package com.example.taskmanagerapp.data.calendar

/**
 * Data class representing a calendar event.
 * This structure maps easily to Google Calendar API later.
 */
data class CalendarEvent(
    val eventId: String,
    val title: String,
    val description: String,
    val startTime: Long,  // Unix timestamp in millis
    val endTime: Long
)

/**
 * Interface for calendar data sources.
 * Can be swapped from FakeCalendarDataSource to GoogleCalendarDataSource later.
 */
interface CalendarDataSource {
    /**
     * Fetch events for the next N days.
     * @param days Number of days to look ahead
     * @return List of calendar events
     */
    suspend fun fetchEvents(days: Int = 7): List<CalendarEvent>

    /**
     * OAuth scopes needed for calendar import.
     * Non-Google implementations can return empty scopes.
     */
    fun importScopes(): List<String> = emptyList()

    /**
     * OAuth scopes needed for calendar sync.
     * Non-Google implementations can return empty scopes.
     */
    fun syncScopes(): List<String> = emptyList()
}
