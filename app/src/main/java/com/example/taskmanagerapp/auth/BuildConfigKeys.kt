package com.example.taskmanagerapp.auth

import android.util.Log
import com.example.taskmanagerapp.BuildConfig

/**
 * Config helper for checking API keys availability.
 * Logs warnings for missing keys but never crashes.
 */
object AppConfig {
    private const val TAG = "AppConfig"

    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val googleWebClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val isDebugBuild: Boolean get() = BuildConfig.DEBUG

    fun hasGoogleClientId(): Boolean {
        val hasId = !googleWebClientId.isNullOrBlank()
        if (!hasId) {
            Log.w(TAG, "GOOGLE_WEB_CLIENT_ID is missing. Google Sign-In will be disabled.")
        }
        return hasId
    }

    fun hasSupabaseConfig(): Boolean {
        val hasConfig = !supabaseUrl.isNullOrBlank() && !supabaseAnonKey.isNullOrBlank()
        if (!hasConfig) {
            Log.w(TAG, "Supabase config is missing (URL or ANON_KEY). Using FakeRemote instead.")
        }
        return hasConfig
    }

    fun logConfigStatus() {
        Log.i(TAG, "=== App Config Status ===")
        Log.i(TAG, "Debug build: $isDebugBuild")
        Log.i(TAG, "Google Client ID: ${if (hasGoogleClientId()) "✓ Present" else "✗ Missing"}")
        Log.i(TAG, "Supabase Config: ${if (hasSupabaseConfig()) "✓ Present" else "✗ Missing"}")
        Log.i(TAG, "=========================")
    }
}

// Deprecated alias for backwards compatibility
@Deprecated("Use AppConfig instead", ReplaceWith("AppConfig"))
typealias BuildConfigKeys = AppConfig
