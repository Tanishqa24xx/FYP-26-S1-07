package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

data class DeleteAccountRequest(
    @SerializedName("user_id") val userId: String
)