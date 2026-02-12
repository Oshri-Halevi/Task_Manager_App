package com.example.taskmanagerapp.auth

import android.content.Context
import android.util.Log
import java.util.concurrent.CancellationException
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import com.example.taskmanagerapp.data.remote.SupabaseProvider

/**
 * Real auth repository backed by Supabase Auth.
 * Exchanges Google ID tokens with Supabase and persists session securely.
 */
class SupabaseAuthRepository(context: Context) : AuthRepository {

    private val storage = EncryptedAuthStorage(context)
    private val supabase get() = SupabaseProvider.client

    companion object {
        private const val TAG = "SupabaseAuth"
    }

    override suspend fun loginWithGoogleIdToken(idToken: String): Session {
        Log.d(TAG, "Exchanging Google ID token with Supabase...")
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }

        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("Supabase auth succeeded but no user returned")

        val supabaseSession = supabase.auth.currentSessionOrNull()
        val session = Session(
            userId = user.id,
            token = supabaseSession?.accessToken,
            refreshToken = supabaseSession?.refreshToken
        )
        storage.save(session)
        Log.d(TAG, "Supabase sign-in successful for user: ${user.id}")
        return session
    }

    override suspend fun signInFake(): Session {
        // Allow demo sign-in even with real repo in debug builds
        if (!AppConfig.isDebugBuild) {
            throw UnsupportedOperationException("Demo sign-in is only available in debug builds")
        }
        Log.d(TAG, "Demo sign-in (local only, skipping Supabase)")
        val session = Session(userId = "demo_user")
        storage.save(session)
        return session
    }

    override suspend fun currentSession(): Session? = storage.load()

    override suspend fun restoreSession(): Session? {
        val stored = storage.load() ?: return null
        return try {
            // Import stored tokens into the Supabase SDK before attempting refresh
            val refreshToken = stored.refreshToken
            if (refreshToken != null) {
                supabase.auth.importAuthToken(
                    stored.token ?: "",
                    refreshToken
                )
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
                val refreshedSession = supabase.auth.currentSessionOrNull()
                val refreshed = stored.copy(
                    token = refreshedSession?.accessToken ?: stored.token,
                    refreshToken = refreshedSession?.refreshToken ?: stored.refreshToken
                )
                storage.save(refreshed)
                Log.d(TAG, "Session restored and refreshed for user: ${stored.userId}")
                refreshed
            } else {
                // No refresh token — can't refresh, just return stored session
                Log.w(TAG, "No refresh token stored, returning cached session")
                stored
            }
        } catch (e: CancellationException) {
            Log.w(TAG, "Session refresh cancelled; keeping stored session", e)
            stored
        } catch (e: Exception) {
            Log.w(TAG, "Session refresh failed, using stored session", e)
            stored
        }
    }

    override suspend fun signOut() {
        // Local sign-out should always win even if remote sign-out fails/cancels.
        storage.clear()
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Supabase sign-out failed (local session already cleared)", e)
        }
    }
}
