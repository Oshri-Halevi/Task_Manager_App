package com.example.taskmanagerapp.data.remote

import com.example.taskmanagerapp.data.local.Task

interface RemoteTaskDataSource {
    // Task lists
    suspend fun fetchLists(): List<RemoteTaskList>
    suspend fun createList(name: String): RemoteTaskList
    suspend fun shareList(listId: Long, memberEmails: List<String>): Boolean

    // Tasks
    suspend fun fetchTasks(listId: Long = DEFAULT_LIST_ID): List<Task>
    suspend fun upsertTask(task: Task, listId: Long = DEFAULT_LIST_ID): String?
    suspend fun deleteTask(taskId: Int, listId: Long = DEFAULT_LIST_ID)
    suspend fun deleteTaskByRemoteId(remoteId: String, listId: Long = DEFAULT_LIST_ID)

    // Members
    suspend fun fetchMembers(listId: Long = DEFAULT_LIST_ID): List<RemoteMember>

    companion object {
        const val DEFAULT_LIST_ID: Long = 1L
    }
}

data class RemoteTaskList(
    val id: Long,
    val name: String
)

data class RemoteMember(
    val userId: String
)
