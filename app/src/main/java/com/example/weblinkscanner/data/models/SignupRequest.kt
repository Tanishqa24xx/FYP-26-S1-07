package com.example.weblinkscanner.data.models

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val plan: String = "free",
    val role: String = "user"
)