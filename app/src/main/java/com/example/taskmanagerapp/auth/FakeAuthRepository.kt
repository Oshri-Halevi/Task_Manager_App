package com.example.taskmanagerapp.auth

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Lightweight in-memory auth substitute so the app can compile without Supabase.
 */
class FakeAuthRepository(context: Context) : AuthRepository {
    private val storage = EncryptedAuthStorage(context)

    override suspend fun loginWithGoogleIdToken(idToken: String): Session {
        // Simulate a network round-trip
        delay(200)
        val session = Session(userId = "fake_user", token = idToken)
        storage.save(session)
        return session
    }

    override suspend fun signInFake(): Session {
        delay(150)
        val session = Session(userId = "fake_user")
        storage.save(session)
        return session
    }

    override suspend fun currentSession(): Session? = storage.load()

    override suspend fun restoreSession(): Session? = storage.load()

    override suspend fun signOut() {
        storage.clear()
    }
}
