package com.example.taskmanagerapp.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.example.taskmanagerapp.data.local.ListDao
import com.example.taskmanagerapp.data.local.ListEntity
import com.example.taskmanagerapp.data.local.TaskDao
import com.example.taskmanagerapp.ui.adapter.ListWithCount
import kotlinx.coroutines.Dispatchers

/**
 * Repository for task-list operations.
 * UI and ViewModel layers should not access DAOs directly.
 */
class ListRepository(
    private val listDao: ListDao,
    private val taskDao: TaskDao
) {
    fun getLists(): LiveData<List<ListEntity>> = listDao.getLists()

    fun getListsWithCounts(lists: List<ListEntity>): LiveData<List<ListWithCount>> = liveData(Dispatchers.IO) {
        val mapped = lists.map { list ->
            val count = runCatching { taskDao.countTasksForList(list.id) }.getOrDefault(0)
            ListWithCount(list, count)
        }
        emit(mapped)
    }

    suspend fun createList(name: String) {
        listDao.upsert(ListEntity(name = name))
    }

    suspend fun getListsSnapshot(): List<ListEntity> = listDao.getAllListsOnce()
}
