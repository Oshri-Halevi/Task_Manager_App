package com.example.taskmanagerapp.data.remote

import android.util.Log
import com.example.taskmanagerapp.data.local.SyncState
import com.example.taskmanagerapp.data.local.Task
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

/**
 * Real Supabase implementation of RemoteTaskDataSource.
 * Uses PostgREST to CRUD tasks, lists, and members.
 */
class SupabaseRemoteTaskDataSource : RemoteTaskDataSource {

    private val db get() = SupabaseProvider.client.postgrest

    companion object {
        private const val TAG = "SupabaseRemote"
        private const val TABLE_TASKS = "tasks"
        private const val TABLE_LISTS = "task_lists"
        private const val TABLE_MEMBERS = "list_members"
    }

    // ---- Serializable DTOs for Supabase JSON mapping ----

    @Serializable
    data class SupabaseTask(
        val id: String? = null,
        val title: String,
        val description: String = "",
        val is_done: Boolean = false,
        val priority: Int = 1,
        val due_date: Long? = null,
        val image_uri: String? = null,
        val list_id: Long = 1,
        val updated_at: Long = System.currentTimeMillis()
    )

    @Serializable
    data class SupabaseList(
        val id: Long? = null,
        val name: String
    )

    @Serializable
    data class SupabaseMember(
        val user_id: String
    )

    // ---- Lists ----

    override suspend fun fetchLists(): List<RemoteTaskList> {
        val rows = db.from(TABLE_LISTS).select().decodeList<SupabaseList>()
        return rows.map { RemoteTaskList(id = it.id ?: 0, name = it.name) }
    }

    override suspend fun createList(name: String): RemoteTaskList {
        val inserted = db.from(TABLE_LISTS)
            .insert(SupabaseList(name = name)) { select() }
            .decodeSingle<SupabaseList>()
        return RemoteTaskList(id = inserted.id ?: 0, name = inserted.name)
    }

    override suspend fun shareList(listId: Long, memberEmails: List<String>): Boolean {
        return try {
            memberEmails.forEach { email ->
                db.from(TABLE_MEMBERS).insert(
                    mapOf("list_id" to listId, "user_id" to email)
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Share list failed", e)
            false
        }
    }

    // ---- Tasks ----

    override suspend fun fetchTasks(listId: Long): List<Task> {
        val rows = db.from(TABLE_TASKS).select {
            filter { eq("list_id", listId) }
        }.decodeList<SupabaseTask>()

        return rows.map { it.toLocalTask() }
    }

    override suspend fun upsertTask(task: Task, listId: Long): String? {
        val dto = task.toSupabaseTask(listId)
        val upserted = db.from(TABLE_TASKS)
            .upsert(dto) { select() }
            .decodeSingle<SupabaseTask>()
        return upserted.id
    }

    override suspend fun deleteTask(taskId: Int, listId: Long) {
        db.from(TABLE_TASKS).delete {
            filter {
                eq("id", taskId.toString())
                eq("list_id", listId)
            }
        }
    }

    override suspend fun deleteTaskByRemoteId(remoteId: String, listId: Long) {
        db.from(TABLE_TASKS).delete {
            filter {
                eq("id", remoteId)
                eq("list_id", listId)
            }
        }
    }

    // ---- Members ----

    override suspend fun fetchMembers(listId: Long): List<RemoteMember> {
        val rows = db.from(TABLE_MEMBERS).select {
            filter { eq("list_id", listId) }
        }.decodeList<SupabaseMember>()
        return rows.map { RemoteMember(userId = it.user_id) }
    }

    // ---- Mappers ----

    private fun SupabaseTask.toLocalTask(): Task = Task(
        id = 0,
        title = title,
        description = description,
        isDone = is_done,
        priority = priority,
        dueDate = due_date,
        imageUri = image_uri,
        listId = list_id,
        remoteId = id,
        updatedAt = updated_at,
        syncState = SyncState.SYNCED
    )

    private fun Task.toSupabaseTask(listId: Long): SupabaseTask = SupabaseTask(
        id = remoteId,
        title = title,
        description = description,
        is_done = isDone,
        priority = priority,
        due_date = dueDate,
        image_uri = imageUri,
        list_id = listId,
        updated_at = updatedAt
    )
}
