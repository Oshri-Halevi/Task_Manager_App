package com.example.taskmanagerapp.data.repository

/**
 * Result of a sync operation.
 */
sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String? = null) : SyncResult()
}
