package com.example.taskmanagerapp.data.repository

import com.example.taskmanagerapp.data.local.MemberDao
import com.example.taskmanagerapp.data.local.MemberEntity
import com.example.taskmanagerapp.data.local.SyncState
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.data.local.TaskDao
import com.example.taskmanagerapp.data.remote.RemoteTaskDataSource

/**
 * Repository is the single source of truth for task data.
 * Data flow: UI -> ViewModel -> Repository -> Room (TaskDao).
 * Remote is used for sync; Room remains the UI source of truth.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val memberDao: MemberDao,
    private val remote: RemoteTaskDataSource
    ) {

    fun getAllTasks() = dao.getAllTasks()

    fun getTasksByList(listId: Long) = dao.getTasksByList(listId)

    suspend fun insert(task: Task) {
        dao.upsert(task.copy(syncState = SyncState.DIRTY, updatedAt = System.currentTimeMillis()))
    }

    suspend fun update(task: Task) {
        dao.upsert(task.copy(syncState = SyncState.DIRTY, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(task: Task) {
        dao.upsert(task.copy(syncState = SyncState.DELETED, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAllTasks() {
        dao.deleteAllTasks()
        // keep remote untouched for safety
    }

    suspend fun getTask(id: Int) = dao.getTaskById(id)

    suspend fun getAllActiveTasksOnce(): List<Task> =
        dao.getAllTasksOnce().filter { it.syncState != SyncState.DELETED }

    suspend fun insertImportedTask(task: Task): Int {
        val importedTask = task.copy(
            syncState = SyncState.DIRTY,
            updatedAt = System.currentTimeMillis()
        )
        return dao.upsert(importedTask).toInt()
    }

    /**
     * SYNC PULL: Fetch remote tasks and merge into local Room database.
     * Matches by remoteId to avoid duplicates. Preserves un-synced local changes.
     */
    suspend fun syncPull() {
        val remoteTasks = remote.fetchTasks()
        for (remoteTask in remoteTasks) {
            val remoteId = remoteTask.remoteId ?: continue
            val existing = dao.getTaskByRemoteId(remoteId)
            if (existing != null) {
                // Only update if local copy is not dirty
                if (existing.syncState == SyncState.SYNCED) {
                    dao.upsert(remoteTask.copy(id = existing.id, syncState = SyncState.SYNCED))
                }
            } else {
                dao.upsert(remoteTask.copy(id = 0, syncState = SyncState.SYNCED))
            }
        }
    }

    /**
     * SYNC PUSH: Upload all dirty/deleted local tasks to remote.
     * After successful push, marks tasks as SYNCED.
     */
    suspend fun syncPush() {
        val localTasks = dao.getDirtyTasks()
        if (localTasks.isEmpty()) return

        val deletedIds = mutableListOf<Int>()
        val syncedIds = mutableListOf<Int>()

        localTasks.forEach { task ->
            when (task.syncState) {
                SyncState.DELETED -> {
                    // Only synced tasks have a remoteId to delete remotely.
                    task.remoteId?.let { remote.deleteTaskByRemoteId(it, task.listId) }
                    deletedIds.add(task.id)
                }
                else -> {
                    val remoteId = remote.upsertTask(
                        task.copy(syncState = SyncState.SYNCED),
                        task.listId
                    ) ?: task.remoteId

                    if (remoteId != null && remoteId != task.remoteId) {
                        dao.upsert(task.copy(remoteId = remoteId, syncState = SyncState.SYNCED))
                    }
                    syncedIds.add(task.id)
                }
            }
        }

        // Remove DELETED tasks and mark others as SYNCED
        if (deletedIds.isNotEmpty()) {
            dao.deleteByIds(deletedIds)
        }
        if (syncedIds.isNotEmpty()) {
            dao.markSyncState(syncedIds, SyncState.SYNCED)
        }
    }

    /**
     * Full sync: push local changes, then pull remote changes.
     */
    suspend fun syncNow(): SyncResult {
        return try {
            syncPush()
            syncPull()
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Error(e.message)
        }
    }

    /**
     * Share the default list with additional members.
     */
    suspend fun shareList(listId: Long, invitedUserId: String, currentUserId: String): Boolean {
        val success = remote.shareList(listId, listOf(invitedUserId))
        if (success) {
            val members = mutableListOf(
                MemberEntity(listId = listId, userId = invitedUserId, role = "member")
            )
            // ensure owner recorded
            members.add(MemberEntity(listId = listId, userId = currentUserId, role = "owner"))
            memberDao.upsertAll(members)
        }
        return success
    }
}
