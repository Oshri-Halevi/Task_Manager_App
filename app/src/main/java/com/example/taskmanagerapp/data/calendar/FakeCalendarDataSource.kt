package com.example.taskmanagerapp.data.calendar

import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Fake implementation of CalendarDataSource.
 * Returns deterministic events for the next N days for testing.
 */
class FakeCalendarDataSource : CalendarDataSource {

    override suspend fun fetchEvents(days: Int): List<CalendarEvent> {
        // Simulate network delay
        delay(500)
        
        val events = mutableListOf<CalendarEvent>()
        val calendar = Calendar.getInstance()
        
        // Generate deterministic events for each day
        for (day in 0 until days) {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, if (day == 0) 0 else 1)
            }
            
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dateStr = "%04d%02d%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            // Morning standup (every weekday)
            if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                val start = calendar.timeInMillis
                events.add(
                    CalendarEvent(
                        eventId = "standup_$dateStr",
                        title = "Daily Standup",
                        description = "Team sync meeting",
                        startTime = start,
                        endTime = start + 30 * 60 * 1000 // 30 minutes
                    )
                )
            }
            
            // Lunch break (every day)
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            val lunchStart = calendar.timeInMillis
            events.add(
                CalendarEvent(
                    eventId = "lunch_$dateStr",
                    title = "Lunch Break",
                    description = "Take a break",
                    startTime = lunchStart,
                    endTime = lunchStart + 60 * 60 * 1000 // 1 hour
                )
            )
            
            // Weekly review (Friday only)
            if (dayOfWeek == Calendar.FRIDAY) {
                calendar.set(Calendar.HOUR_OF_DAY, 14)
                val reviewStart = calendar.timeInMillis
                events.add(
                    CalendarEvent(
                        eventId = "review_$dateStr",
                        title = "Weekly Review",
                        description = "Review weekly progress and plan next week",
                        startTime = reviewStart,
                        endTime = reviewStart + 60 * 60 * 1000 // 1 hour
                    )
                )
            }
            
            // Reset hour for next day iteration
            calendar.set(Calendar.HOUR_OF_DAY, 9)
        }
        
        return events
    }
}
