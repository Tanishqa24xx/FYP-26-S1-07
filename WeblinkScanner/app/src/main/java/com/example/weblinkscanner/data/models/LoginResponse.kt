package com.example.weblinkscanner.data.models

data class LoginResponse(
    val access_token: String?,
    val user: String?,
    val name: String?,
    val plan: String?,
    val user_id: String?       // ← added
)
