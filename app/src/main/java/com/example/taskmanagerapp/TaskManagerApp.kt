package com.example.taskmanagerapp

import android.app.Application
import android.util.Log
import com.example.taskmanagerapp.auth.AppConfig
import com.example.taskmanagerapp.auth.AuthRepository
import com.example.taskmanagerapp.auth.FakeAuthRepository
import com.example.taskmanagerapp.auth.SupabaseAuthRepository
import com.example.taskmanagerapp.data.local.AppDatabase
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.data.repository.CalendarRepository
import com.example.taskmanagerapp.data.repository.ListRepository
import com.example.taskmanagerapp.data.remote.RemoteTaskDataSource
import com.example.taskmanagerapp.data.remote.FakeRemoteTaskDataSource
import com.example.taskmanagerapp.data.remote.SupabaseRemoteTaskDataSource
import com.example.taskmanagerapp.data.calendar.CalendarDataSource
import com.example.taskmanagerapp.data.calendar.FakeCalendarDataSource
import com.example.taskmanagerapp.data.calendar.GoogleCalendarDataSource
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModelFactory
import com.example.taskmanagerapp.ui.viewmodel.ListViewModelFactory
import com.example.taskmanagerapp.auth.AuthViewModelFactory

class TaskManagerApp : Application() {

    companion object {
        private const val TAG = "TaskManagerApp"
    }

    private val database by lazy { AppDatabase.getDatabase(this) }

    // --- Conditional DI: Real when keys are present, Fake otherwise ---

    private val remoteDataSource: RemoteTaskDataSource by lazy {
        if (AppConfig.hasSupabaseConfig()) {
            Log.i(TAG, "Using REAL Supabase remote data source")
            SupabaseRemoteTaskDataSource()
        } else {
            Log.i(TAG, "Using FAKE remote data source (Supabase keys missing)")
            FakeRemoteTaskDataSource()
        }
    }

    private val calendarDataSource: CalendarDataSource by lazy {
        if (AppConfig.hasGoogleClientId()) {
            Log.i(TAG, "Using REAL Google Calendar data source")
            GoogleCalendarDataSource(this)
        } else {
            Log.i(TAG, "Using FAKE calendar data source (Google client ID missing)")
            FakeCalendarDataSource()
        }
    }

    val authRepository: AuthRepository by lazy {
        if (AppConfig.hasSupabaseConfig()) {
            Log.i(TAG, "Using REAL Supabase auth repository")
            SupabaseAuthRepository(this)
        } else {
            Log.i(TAG, "Using FAKE auth repository (Supabase keys missing)")
            FakeAuthRepository(this)
        }
    }

    val taskRepository by lazy { TaskRepository(database.taskDao(), database.memberDao(), remoteDataSource) }
    val listRepository by lazy { ListRepository(database.listDao(), database.taskDao()) }
    val calendarRepository by lazy {
        CalendarRepository(
            calendarDataSource,
            database.calendarImportMapDao(),
            listRepository,
            taskRepository,
            getString(R.string.list_name_imported)
        )
    }

    val taskViewModelFactory by lazy { TaskViewModelFactory(taskRepository, calendarRepository) }
    val listViewModelFactory by lazy { ListViewModelFactory(listRepository) }
    val authViewModelFactory by lazy { AuthViewModelFactory(authRepository) }

    override fun onCreate() {
        super.onCreate()
        AppConfig.logConfigStatus()
    }
}
