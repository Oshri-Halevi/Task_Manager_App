package com.example.taskmanagerapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CalendarImportMapDao {

    @Query("SELECT eventId FROM calendar_import_map")
    suspend fun getAllImportedEventIds(): List<String>

    @Query("SELECT * FROM calendar_import_map")
    suspend fun getAllMappings(): List<CalendarImportMapEntity>

    @Query("SELECT eventId FROM calendar_import_map WHERE taskId = :taskId ORDER BY importedAt DESC LIMIT 1")
    suspend fun getEventIdByTaskId(taskId: Int): String?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: CalendarImportMapEntity): Long

    @Query("DELETE FROM calendar_import_map WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Int)

    @Query("DELETE FROM calendar_import_map WHERE taskId NOT IN (SELECT id FROM tasks)")
    suspend fun deleteOrphans()

    @Query("DELETE FROM calendar_import_map")
    suspend fun deleteAll()
}
