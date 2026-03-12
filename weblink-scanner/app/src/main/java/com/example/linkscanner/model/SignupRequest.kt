package com.example.linkscanner.model

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val plan: String = "free"
)