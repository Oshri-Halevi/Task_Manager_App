package com.example.taskmanagerapp.data.remote

import com.example.taskmanagerapp.auth.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton Supabase client.
 * Only initialised when AppConfig.hasSupabaseConfig() is true.
 */
object SupabaseProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = AppConfig.supabaseUrl,
            supabaseKey = AppConfig.supabaseAnonKey
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
