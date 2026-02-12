package com.example.taskmanagerapp.auth

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val userId: String,
    val token: String? = null,
    val refreshToken: String? = null
)
