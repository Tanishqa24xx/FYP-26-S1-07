package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

data class EditProfileRequest(
    @SerializedName("user_id") val userId: String,
    val name: String
)
