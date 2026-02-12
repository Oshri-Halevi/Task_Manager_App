package com.example.taskmanagerapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps external calendar/task IDs to local task IDs.
 * Used for duplicate-safe import and for upsert sync to Google Calendar.
 */
@Entity(
    tableName = "calendar_import_map",
    indices = [Index(value = ["eventId"], unique = true)]
)
data class CalendarImportMapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val taskId: Int,
    val importedAt: Long = System.currentTimeMillis()
)
