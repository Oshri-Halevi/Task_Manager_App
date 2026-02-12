package com.example.taskmanagerapp.auth

/**
 * Simple helper wrapper around encrypted storage.
 */
class SessionStorageAdapter(private val secureStorage: EncryptedAuthStorage) {
    suspend fun saveSession(session: Session) = secureStorage.save(session)
    suspend fun loadSession(): Session? = secureStorage.load()
    suspend fun deleteSession() = secureStorage.clear()
}
