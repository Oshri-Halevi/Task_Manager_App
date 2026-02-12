package com.example.taskmanagerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.R
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.data.repository.CalendarRepository
import com.example.taskmanagerapp.data.repository.CalendarImportResult
import com.example.taskmanagerapp.data.repository.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class FilterType { ALL, ACTIVE, DONE }
enum class SortType { DATE, PRIORITY }

sealed class SyncResultState {
    data class Success(
        val calendarCreated: Int,
        val calendarUpdated: Int,
        val calendarSkipped: Int
    ) : SyncResultState()
    data class Error(val message: String?) : SyncResultState()
}

/**
 * UI state holder for tasks.
 *
 * Data flow:
 * UI -> TaskViewModel -> Repositories -> Room/remote sources.
 * UI never talks to DAOs directly.
 */
class TaskViewModel(
    private val repository: TaskRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TaskViewModel"
    }

    private val _filter = MutableLiveData(FilterType.ALL)
    val filter: LiveData<FilterType> = _filter

    private val _sort = MutableLiveData(SortType.DATE)
    val sort: LiveData<SortType> = _sort

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<Int?>()
    val errorMessage: LiveData<Int?> = _errorMessage

    private val _shareLoading = MutableLiveData(false)
    val shareLoading: LiveData<Boolean> = _shareLoading

    private val _shareResult = MutableLiveData<Boolean?>()
    val shareResult: LiveData<Boolean?> = _shareResult

    private val _selectedTask = MutableLiveData<Task?>()
    val selectedTask: LiveData<Task?> = _selectedTask

    private val _syncLoading = MutableLiveData(false)
    val syncLoading: LiveData<Boolean> = _syncLoading

    private val _syncResult = MutableLiveData<SyncResultState?>()
    val syncResult: LiveData<SyncResultState?> = _syncResult

    private val _calendarImportLoading = MutableLiveData(false)
    val calendarImportLoading: LiveData<Boolean> = _calendarImportLoading

    private val _calendarImportResult = MutableLiveData<CalendarImportResult?>()
    val calendarImportResult: LiveData<CalendarImportResult?> = _calendarImportResult

    private val _calendarImportError = MutableLiveData<String?>()
    val calendarImportError: LiveData<String?> = _calendarImportError

    private val allTasks = repository.getAllTasks()

    fun getTasksByList(listId: Long): LiveData<List<Task>> = _filter.switchMap { currentFilter ->
        _sort.switchMap { currentSort ->
            repository.getTasksByList(listId).map { list ->
                _isLoading.value = false
                val filtered: List<Task> = when (currentFilter) {
                    FilterType.ALL -> list
                    FilterType.ACTIVE -> list.filter { !it.isDone }
                    FilterType.DONE -> list.filter { it.isDone }
                }
                applySort(filtered, currentSort)
            }
        }
    }

    val tasks: LiveData<List<Task>> = _filter.switchMap { currentFilter ->
        _sort.switchMap { currentSort ->
            allTasks.map { list ->
                _isLoading.value = false
                val filtered: List<Task> = when (currentFilter) {
                    FilterType.ALL -> list
                    FilterType.ACTIVE -> list.filter { !it.isDone }
                    FilterType.DONE -> list.filter { it.isDone }
                }
                applySort(filtered, currentSort)
            }
        }
    }

    val todayTasks: LiveData<List<Task>> = MediatorLiveData<List<Task>>().apply {
        fun updateTodayList(tasks: List<Task>?, sort: SortType?) {
            if (tasks == null || sort == null) return
            val (start, end) = todayRange()
            val filtered = tasks.filter { task ->
                task.dueDate != null && task.dueDate in start..end
            }
            value = applySort(filtered, sort)
            _isLoading.value = false
        }

        addSource(allTasks) { updateTodayList(it, _sort.value) }
        addSource(_sort) { updateTodayList(allTasks.value, it) }
    }

    fun insert(task: Task) {
        setProcessing(true)
        viewModelScope.launch {
            runCatching { repository.insert(task) }
                .onFailure { _errorMessage.postValue(R.string.error_save_task) }
            setProcessing(false)
        }
    }

    fun update(task: Task) {
        setProcessing(true)
        viewModelScope.launch {
            runCatching { repository.update(task) }
                .onFailure { _errorMessage.postValue(R.string.error_update_task) }
            setProcessing(false)
        }
    }

    fun delete(task: Task) {
        setProcessing(true)
        viewModelScope.launch {
            runCatching { repository.delete(task) }
                .onFailure { _errorMessage.postValue(R.string.error_delete_task) }
            setProcessing(false)
        }
    }

    private val _deleteAllDone = MutableLiveData<Boolean?>()
    val deleteAllDone: LiveData<Boolean?> = _deleteAllDone

    fun deleteAllTasks() {
        setProcessing(true)
        viewModelScope.launch {
            runCatching {
                repository.deleteAllTasks()
                calendarRepository.clearImportHistory()
            }
                .onSuccess { _deleteAllDone.postValue(true) }
                .onFailure { _errorMessage.postValue(R.string.error_delete_all_tasks) }
            setProcessing(false)
        }
    }

    fun clearDeleteAllDone() {
        _deleteAllDone.value = null
    }

    fun loadTask(taskId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            runCatching { repository.getTask(taskId) }
                .onSuccess { _selectedTask.postValue(it) }
                .onFailure { _errorMessage.postValue(R.string.error_load_task) }
            _isLoading.postValue(false)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearShareResult() {
        _shareResult.value = null
    }

    fun shareList(listId: Long, invitedUserId: String, currentUserId: String) {
        _shareLoading.value = true
        viewModelScope.launch {
            runCatching { repository.shareList(listId, invitedUserId, currentUserId) }
                .onSuccess { _shareResult.postValue(it) }
                .onFailure { _shareResult.postValue(false) }
            _shareLoading.postValue(false)
        }
    }

    fun syncNow() {
        _syncLoading.value = true
        viewModelScope.launch {
            when (val cloudResult = withContext(Dispatchers.IO) { repository.syncNow() }) {
                is SyncResult.Error -> {
                    _syncResult.postValue(SyncResultState.Error(cloudResult.message))
                }
                is SyncResult.Success -> {
                    runCatching { withContext(Dispatchers.IO) { calendarRepository.syncTasksToCalendar() } }
                        .onSuccess { calendarResult ->
                            _syncResult.postValue(
                                SyncResultState.Success(
                                    calendarCreated = calendarResult.created,
                                    calendarUpdated = calendarResult.updated,
                                    calendarSkipped = calendarResult.skipped
                                )
                            )
                        }
                        .onFailure { error ->
                            val type = error::class.simpleName ?: "Error"
                            val message = error.message?.takeIf { it.isNotBlank() } ?: type
                            _syncResult.postValue(SyncResultState.Error(message))
                        }
                }
            }
            _syncLoading.postValue(false)
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }

    /**
     * Import events from calendar as tasks.
     * Creates an "Imported" list if not exists.
     */
    fun importCalendar() {
        _calendarImportLoading.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { calendarRepository.importCalendar() }
            }
            result
                .onSuccess { result ->
                    _calendarImportResult.postValue(result)
                }
                .onFailure { error ->
                    Log.e(TAG, "Calendar import failed", error)
                    val type = error::class.simpleName ?: "Error"
                    val message = error.message?.takeIf { it.isNotBlank() }
                    _calendarImportError.postValue(
                        if (message != null) "$type: $message" else type
                    )
                }
            _calendarImportLoading.postValue(false)
        }
    }

    fun clearCalendarImportResult() {
        _calendarImportResult.value = null
    }

    fun clearCalendarImportError() {
        _calendarImportError.value = null
    }

    fun requiredCalendarImportScopes(): List<String> = calendarRepository.requiredImportScopes()

    fun requiredCalendarSyncScopes(): List<String> = calendarRepository.requiredSyncScopes()

    fun setFilter(type: FilterType) {
        _filter.value = type
    }

    fun setSort(type: SortType) {
        _sort.value = type
    }

    fun markListLoading() {
        _isLoading.value = true
    }

    fun markListLoaded() {
        _isLoading.value = false
    }

    private fun applySort(tasks: List<Task>, sort: SortType): List<Task> =
        when (sort) {
            SortType.DATE -> tasks.sortedWith(
                compareBy<Task> { it.isDone }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
            )
            SortType.PRIORITY -> tasks.sortedWith(
                compareBy<Task> { it.isDone }
                    .thenByDescending { it.priority }
            )
        }

    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        return start to end
    }

    private fun setProcessing(active: Boolean) {
        _isProcessing.postValue(active)
    }
}
