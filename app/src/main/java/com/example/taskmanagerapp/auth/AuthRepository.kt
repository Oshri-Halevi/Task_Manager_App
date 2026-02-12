package com.example.taskmanagerapp.auth

/**
 * Interface for authentication operations.
 * Implemented by FakeAuthRepository (offline/demo) and SupabaseAuthRepository (real).
 */
interface AuthRepository {
    suspend fun loginWithGoogleIdToken(idToken: String): Session
    suspend fun signInFake(): Session
    suspend fun currentSession(): Session?
    suspend fun restoreSession(): Session?
    suspend fun signOut()
}
