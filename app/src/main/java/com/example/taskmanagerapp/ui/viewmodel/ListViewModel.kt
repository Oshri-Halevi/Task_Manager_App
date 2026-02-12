package com.example.taskmanagerapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.R
import com.example.taskmanagerapp.data.local.ListEntity
import com.example.taskmanagerapp.data.repository.ListRepository
import com.example.taskmanagerapp.ui.adapter.ListWithCount
import kotlinx.coroutines.launch

/**
 * ViewModel for task-list UI state.
 * Communicates with ListRepository only (no direct DAO access).
 */
class ListViewModel(
    private val repository: ListRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<Int?>()
    val errorMessage: LiveData<Int?> = _errorMessage

    val lists: LiveData<List<ListEntity>> = repository.getLists()

    val listsWithCounts: LiveData<List<ListWithCount>> = lists.switchMap(repository::getListsWithCounts)

    fun createList(name: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.createList(name)
            } catch (e: Exception) {
                _errorMessage.postValue(R.string.error_create_list)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
