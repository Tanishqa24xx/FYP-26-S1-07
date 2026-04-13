package com.example.weblinkscanner.data.api

import com.example.weblinkscanner.data.models.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface NewApiService {

    // --- AUTH (legacy Call<> style used by Login/Signup/EditProfile screens) ---
    @POST("login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("signup")
    fun signup(
        @Body request: SignupRequest
    ): Call<SignupResponse>

    @POST("forgot-password")
    fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Call<ForgotPasswordResponse>

    @POST("change-password")
    fun changePassword(
        @Body request: ChangePasswordRequest
    ): Call<ChangePasswordResponse>

    // --- PLANS ---
    @GET("plan/")
    suspend fun getMyPlan(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String
    ): Response<UserPlanResponse>

    @GET("plan/all")
    suspend fun getAllPlans(): Response<AllPlansResponse>

    @POST("plan/upgrade")
    suspend fun upgradePlan(
        @Header("Authorization") token: String,
        @Body request: UpgradePlanRequest
    ): Response<UpgradePlanResponse>

    // --- SCANNING ---
    @POST("scan/url")
    suspend fun scanUrl(
        @Header("Authorization") token: String,
        @Body request: NewScanRequest
    ): Response<NewScanResponse>

    @POST("scan/camera")
    suspend fun scanCamera(
        @Header("Authorization") token: String,
        @Body request: CameraScanRequest
    ): Response<CameraScanResponse>

    @POST("scan/qr")
    suspend fun scanQr(
        @Header("Authorization") token: String,
        @Body request: QRScanRequest
    ): Response<QRScanResponse>

    // --- SANDBOX ---
    @POST("sandbox/analyse")
    suspend fun analyseSandbox(
        @Header("Authorization") token: String,
        @Body request: SandboxRequest
    ): Response<SandboxReport>

    // --- SAVED LINKS ---
    @POST("saved-links/")
    suspend fun saveLink(
        @Header("Authorization") token: String,
        @Body request: SaveLinkRequest
    ): Response<Map<String, String>>

    @GET("saved-links/{userId}")
    suspend fun getSavedLinks(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<SavedLinksResponse>

    @POST("saved-links/delete")
    suspend fun deleteLinks(
        @Header("Authorization") token: String,
        @Body ids: List<String>
    ): Response<Map<String, String>>

    @GET("faq/")
    suspend fun getFaqs(
        @Header("Authorization") token: String
    ): Response<List<@JvmSuppressWildcards Map<String, String>>>

    // --- SCAN HISTORY ---
    @POST("scan/history/delete")
    suspend fun deleteHistoryItems(
        @Header("Authorization") token: String,
        @Body ids: List<String>
    ): Response<Map<String, String>>

    @GET("scan/history/{userId}")
    suspend fun getScanHistory(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<List<NewScanResponse>>

    @DELETE("delete-account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String
    ): Response<Map<String, String>>
}
