package com.example.taskmanagerapp.auth

import androidx.annotation.StringRes
import com.example.taskmanagerapp.R

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String = "user_1") : AuthState()
    data class Error(@StringRes val message: Int = R.string.error_auth_failed) : AuthState()
}
