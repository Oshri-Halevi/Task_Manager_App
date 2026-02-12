package com.example.taskmanagerapp.data.remote

import com.example.taskmanagerapp.data.local.Task
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeRemoteTaskDataSource : RemoteTaskDataSource {

    @Volatile
    var shouldFail: Boolean = false

    private val lists = mutableListOf(
        RemoteTaskList(RemoteTaskDataSource.DEFAULT_LIST_ID, "Default")
    )
    private val members = mutableMapOf(
        RemoteTaskDataSource.DEFAULT_LIST_ID to mutableListOf(RemoteMember("owner"))
    )
    private val tasks = mutableMapOf<Long, MutableList<Task>>().apply {
        put(RemoteTaskDataSource.DEFAULT_LIST_ID, mutableListOf())
    }

    override suspend fun fetchLists(): List<RemoteTaskList> = simulate {
        lists.toList()
    }

    override suspend fun createList(name: String): RemoteTaskList = simulate {
        val newList = RemoteTaskList(id = Random.nextLong(), name = name)
        lists.add(newList)
        tasks[newList.id] = mutableListOf()
        members[newList.id] = mutableListOf()
        newList
    }

    override suspend fun shareList(listId: Long, memberEmails: List<String>): Boolean = simulate {
        val listMembers = members[listId] ?: mutableListOf<RemoteMember>().also {
            members[listId] = it
        }
        memberEmails.forEach { listMembers.add(RemoteMember(it)) }
        true
    }

    override suspend fun fetchTasks(listId: Long): List<Task> = simulate {
        tasks[listId]?.toList() ?: emptyList()
    }

    override suspend fun upsertTask(task: Task, listId: Long) = simulate<String?> {
        val list = tasks[listId] ?: mutableListOf<Task>().also { tasks[listId] = it }
        val index = list.indexOfFirst { it.remoteId == task.remoteId && task.remoteId != null || it.id == task.id }
        val remoteId = task.remoteId ?: "fake_remote_${Random.nextLong(1_000_000_000)}"
        val taskWithRemoteId = task.copy(listId = listId, remoteId = remoteId)
        if (index >= 0) {
            list[index] = taskWithRemoteId
        } else {
            list.add(taskWithRemoteId)
        }
        remoteId
    }

    override suspend fun deleteTask(taskId: Int, listId: Long) = simulate<Unit> {
        tasks[listId]?.removeAll { it.id == taskId }
        Unit
    }

    override suspend fun deleteTaskByRemoteId(remoteId: String, listId: Long) = simulate<Unit> {
        tasks[listId]?.removeAll { it.remoteId == remoteId }
        Unit
    }

    override suspend fun fetchMembers(listId: Long): List<RemoteMember> = simulate {
        members[listId]?.toList() ?: emptyList()
    }

    private suspend fun <T> simulate(block: () -> T): T {
        delay(Random.nextLong(300, 800))
        if (shouldFail) throw IllegalStateException("Simulated network failure")
        return block()
    }
}
