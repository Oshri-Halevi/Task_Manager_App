package com.example.taskmanagerapp.data.remote

import com.example.taskmanagerapp.data.local.SyncState
import com.example.taskmanagerapp.data.local.Task

/**
 * Data Transfer Object for remote API communication.
 * This separates the local Room entity from the network layer,
 * allowing the API to be swapped (e.g., to real Supabase) without changing local models.
 */
data class RemoteTaskDto(
    val remoteId: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val priority: Int,
    val dueDate: Long?,
    val imageUri: String?,
    val listId: Long,
    val updatedAt: Long
)

/**
 * Mapper functions between Task (Room entity) and RemoteTaskDto (API model).
 */
object TaskMapper {

    /**
     * Convert a local Task to RemoteTaskDto for API upload.
     */
    fun Task.toRemoteDto(): RemoteTaskDto = RemoteTaskDto(
        remoteId = remoteId ?: "local_$id",
        title = title,
        description = description,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        imageUri = imageUri,
        listId = listId,
        updatedAt = updatedAt
    )

    /**
     * Convert RemoteTaskDto to local Task for Room storage.
     */
    fun RemoteTaskDto.toLocalTask(): Task = Task(
        id = 0, // Let Room auto-generate
        title = title,
        description = description,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        imageUri = imageUri,
        listId = listId,
        remoteId = remoteId,
        updatedAt = updatedAt,
        syncState = SyncState.SYNCED
    )
}
