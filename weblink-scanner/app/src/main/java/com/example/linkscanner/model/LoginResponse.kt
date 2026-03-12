package com.example.linkscanner.model

data class LoginResponse(
    val access_token: String?,
    val user: String?,
    val name: String?,
    val plan: String?
)