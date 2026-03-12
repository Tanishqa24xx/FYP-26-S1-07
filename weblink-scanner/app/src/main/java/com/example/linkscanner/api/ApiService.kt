package com.example.linkscanner.api

import com.example.linkscanner.model.ChangePasswordRequest
import com.example.linkscanner.model.ChangePasswordResponse
import com.example.linkscanner.model.ForgotPasswordRequest
import com.example.linkscanner.model.ForgotPasswordResponse
import com.example.linkscanner.model.LoginRequest
import com.example.linkscanner.model.LoginResponse
import com.example.linkscanner.model.SignupRequest
import com.example.linkscanner.model.SignupResponse
import com.example.linkscanner.model.ScanRequest
import com.example.linkscanner.model.ScanResponse

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("signup")
    fun signup(
        @Body request: SignupRequest
    ): Call<SignupResponse>

    @POST("scan")
    fun scanUrl(
        @Body request: ScanRequest
    ): Call<ScanResponse>

    @POST("forgot-password")
    fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Call<ForgotPasswordResponse>

    @POST("change-password")
    fun changePassword(
        @Body request: ChangePasswordRequest
    ): Call<ChangePasswordResponse>

}