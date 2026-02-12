package com.example.taskmanagerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.data.repository.CalendarRepository

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val calendarRepository: CalendarRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository, calendarRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
