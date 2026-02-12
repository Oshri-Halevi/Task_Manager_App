package com.example.taskmanagerapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskmanagerapp.data.local.SyncState.SYNCED

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["listId"])]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val imageUri: String? = null,
    val priority: Int = 1, // 0 = Low, 1 = Normal, 2 = High
    val dueDate: Long? = null, // Timestamp in milliseconds
    @ColumnInfo(defaultValue = "1")
    val listId: Long = DEFAULT_LIST_ID,
    val remoteId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncState: SyncState = SYNCED
) {
    companion object {
        const val DEFAULT_LIST_ID = 1L
    }
}

enum class SyncState { SYNCED, DIRTY, DELETED }
