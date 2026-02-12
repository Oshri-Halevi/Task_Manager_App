package com.example.taskmanagerapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.taskmanagerapp.data.local.SyncState


@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE syncState != 'DELETED' ORDER BY id DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND syncState != 'DELETED' ORDER BY id DESC")
    fun getTasksByList(listId: Long): LiveData<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE syncState IN ('DIRTY','DELETED')")
    suspend fun getDirtyTasks(): List<Task>

    @Query("UPDATE tasks SET syncState = :state WHERE id IN (:ids)")
    suspend fun markSyncState(ids: List<Int>, state: SyncState = SyncState.SYNCED)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): Task?

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getTaskByRemoteId(remoteId: String): Task?

    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId AND syncState != 'DELETED'")
    suspend fun countTasksForList(listId: Long): Int
}
