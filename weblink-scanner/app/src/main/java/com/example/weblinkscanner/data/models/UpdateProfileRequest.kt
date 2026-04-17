package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("user_id") val userId: String,
    val name: String,
    val email: String
)
