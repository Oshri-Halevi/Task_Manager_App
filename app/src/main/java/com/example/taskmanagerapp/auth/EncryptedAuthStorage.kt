package com.example.taskmanagerapp.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Minimal encrypted storage for auth session without JSON serialization.
 */
class EncryptedAuthStorage(context: Context) {

    data class StoredSession(
        val accessToken: String?,
        val refreshToken: String?,
        val userId: String?
    )

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "auth_storage",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(session: Session) {
        val stored = StoredSession(
            accessToken = session.token,
            refreshToken = session.refreshToken,
            userId = session.userId
        )
        prefs.edit()
            .putString(KEY_ACCESS, stored.accessToken)
            .putString(KEY_REFRESH, stored.refreshToken)
            .putString(KEY_USER_ID, stored.userId)
            .commit()
    }

    fun load(): Session? {
        val stored = StoredSession(
            accessToken = prefs.getString(KEY_ACCESS, null),
            refreshToken = prefs.getString(KEY_REFRESH, null),
            userId = prefs.getString(KEY_USER_ID, null)
        )
        return stored.userId?.let {
            Session(userId = it, token = stored.accessToken, refreshToken = stored.refreshToken)
        }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_USER_ID)
            .commit()
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }
}
