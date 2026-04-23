package com.example.weblinkscanner.data.api

import com.example.weblinkscanner.data.models.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface NewApiService {

    // ── AUTH (legacy Call<> style used by Login/Signup/EditProfile screens) ───
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

    @PUT("update-profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<Map<String, String>>

    // ── PLANS ─────────────────────────────────────────────────────────────────
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
        @Query("user_id") userId: String,
        @Body request: UpgradePlanRequest
    ): Response<UpgradePlanResponse>

    // ── SCANNING ──────────────────────────────────────────────────────────────
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

    // ── SANDBOX ───────────────────────────────────────────────────────────────
    @POST("sandbox/analyse")
    suspend fun analyseSandbox(
        @Header("Authorization") token: String,
        @Body request: SandboxRequest
    ): Response<SandboxReport>

    @GET("sandbox/{scanId}")
    suspend fun getSandboxReport(
        @Header("Authorization") token: String,
        @Path("scanId") scanId: String
    ): Response<SandboxReport>

    // ── SAVED LINKS ───────────────────────────────────────────────────────────
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

    @POST("saved-links/recheck")
    suspend fun recheckSavedLinks(
        @Header("Authorization") token: String,
        @Body request: RecheckRequest
    ): Response<RecheckResponse>

    @POST("saved-links/rescan")
    suspend fun rescanSavedLinks(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String,
        @Query("force") force: Boolean = false,
        @Query("selected_ids") selectedIds: List<String> = emptyList()
    ): Response<RescanResponse>

    @GET("scan/export")
    suspend fun exportScanHistory(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String,
        @Query("fmt") fmt: String
    ): Response<ResponseBody>

    @GET("faq/")
    suspend fun getFaqs(
        @Header("Authorization") token: String
    ): Response<List<@JvmSuppressWildcards Map<String, String>>>

    // ── SCAN HISTORY ──────────────────────────────────────────────────────────
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

    // ── ADMIN ─────────────────────────────────────────────────────────────────
    @GET("admin/stats")
    suspend fun getAdminStats(
        @Header("Authorization") token: String
    ): Response<AdminStatsResponse>

    @GET("admin/users")
    suspend fun getAdminUsers(
        @Header("Authorization") token: String,
        @Query("q") search: String? = null
    ): Response<AdminUsersResponse>

    @POST("admin/users")
    suspend fun createAdminUser(
        @Header("Authorization") token: String,
        @Body request: CreateAdminUserRequest
    ): Response<AdminUser>

    @GET("admin/users/{userId}")
    suspend fun getAdminUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<AdminUser>

    @PUT("admin/users/{userId}")
    suspend fun updateAdminUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Body request: UpdateAdminUserRequest
    ): Response<AdminUser>

    @POST("admin/users/{userId}/suspend")
    suspend fun suspendUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    @POST("admin/users/{userId}/reactivate")
    suspend fun reactivateUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    @POST("admin/users/{userId}/lock")
    suspend fun lockUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    @POST("admin/users/{userId}/unlock")
    suspend fun unlockUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    @POST("admin/users/{userId}/approve")
    suspend fun approveUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    @POST("admin/users/{userId}/reject")
    suspend fun rejectUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    // ── ADMIN PROFILES ────────────────────────────────────────────────────────
    @GET("admin/profiles")
    suspend fun getProfiles(
        @Header("Authorization") token: String,
        @Query("q") search: String? = null
    ): Response<UserProfilesResponse>

    @POST("admin/profiles")
    suspend fun createProfile(
        @Header("Authorization") token: String,
        @Body request: CreateProfileRequest
    ): Response<UserProfile>

    @GET("admin/profiles/{profileId}")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Path("profileId") profileId: String
    ): Response<UserProfile>

    @PUT("admin/profiles/{profileId}")
    suspend fun updateAdminProfile(
        @Header("Authorization") token: String,
        @Path("profileId") profileId: String,
        @Body request: AdminProfileUpdateRequest
    ): Response<UserProfile>

    @POST("admin/profiles/{profileId}/suspend")
    suspend fun suspendProfile(
        @Header("Authorization") token: String,
        @Path("profileId") profileId: String
    ): Response<Map<String, String>>

    @POST("admin/profiles/{profileId}/reactivate")
    suspend fun reactivateProfile(
        @Header("Authorization") token: String,
        @Path("profileId") profileId: String
    ): Response<Map<String, String>>

    @POST("admin/users/{userId}/assign-profile")
    suspend fun assignProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Body request: AssignProfileRequest
    ): Response<Map<String, String>>

    @DELETE("admin/users/{userId}/assign-profile")
    suspend fun removeProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, String>>

    // ── ADMIN MONITOR / REPORTS ───────────────────────────────────────────────
    @GET("admin/security")
    suspend fun getSecurityUsers(
        @Header("Authorization") token: String
    ): Response<AdminUsersResponse>

    @GET("admin/scans")
    suspend fun getAdminScans(
        @Header("Authorization") token: String,
        @Query("verdict") verdict: String? = null
    ): Response<AdminScanRecordsResponse>

    @GET("admin/flagged")
    suspend fun getFlaggedLinks(
        @Header("Authorization") token: String,
        @Query("verdict") verdict: String? = null
    ): Response<AdminScanRecordsResponse>

    @GET("admin/audit")
    suspend fun getAuditLog(
        @Header("Authorization") token: String
    ): Response<AuditLogResponse>

    @GET("admin/subscriptions")
    suspend fun getSubscriptions(
        @Header("Authorization") token: String
    ): Response<SubscriptionResponse>

    // ── PLATFORM MANAGER ──────────────────────────────────────────────────────
    @GET("platform/plans")
    suspend fun getPMPlans(
        @Header("Authorization") token: String
    ): Response<PMPlansResponse>

    @POST("platform/plans")
    suspend fun createPMPlan(
        @Header("Authorization") token: String,
        @Body request: PMCreatePlanRequest
    ): Response<PMSubscriptionPlan>

    @GET("platform/plans/{planId}")
    suspend fun getPMPlan(
        @Header("Authorization") token: String,
        @Path("planId") planId: String
    ): Response<PMSubscriptionPlan>

    @PUT("platform/plans/{planId}")
    suspend fun updatePMPlan(
        @Header("Authorization") token: String,
        @Path("planId") planId: String,
        @Body request: PMUpdatePlanRequest
    ): Response<PMSubscriptionPlan>

    @POST("platform/plans/{planId}/suspend")
    suspend fun suspendPMPlan(
        @Header("Authorization") token: String,
        @Path("planId") planId: String
    ): Response<Map<String, String>>

    @POST("platform/plans/{planId}/activate")
    suspend fun activatePMPlan(
        @Header("Authorization") token: String,
        @Path("planId") planId: String
    ): Response<Map<String, String>>

    @GET("platform/analytics/overview")
    suspend fun getPMAnalyticsOverview(
        @Header("Authorization") token: String
    ): Response<PMAnalyticsOverview>

    @GET("platform/analytics/features")
    suspend fun getPMFeatureAnalytics(
        @Header("Authorization") token: String
    ): Response<PMFeatureAnalytics>

    @GET("platform/analytics/reports")
    suspend fun generatePMReport(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date")   endDate: String?   = null
    ): Response<PMReportResponse>

    @GET("platform/support")
    suspend fun getPMSupportRequests(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): Response<PMSupportRequestsResponse>

    @GET("platform/support/{requestId}")
    suspend fun getPMSupportRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String
    ): Response<PMSupportRequest>

    @POST("platform/support/{requestId}/reply")
    suspend fun replyToPMSupport(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String,
        @Body request: PMCreateReplyRequest
    ): Response<PMSupportReply>

    @PUT("platform/support/{requestId}/status")
    suspend fun updatePMSupportStatus(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String,
        @Body request: PMUpdateSupportStatusRequest
    ): Response<Map<String, String>>

    @GET("platform/faq")
    suspend fun getPMFaqs(
        @Header("Authorization") token: String
    ): Response<PMFaqResponse>

    @POST("platform/faq")
    suspend fun createPMFaq(
        @Header("Authorization") token: String,
        @Body request: PMCreateFaqRequest
    ): Response<PMFaqItem>

    @PUT("platform/faq/{faqId}")
    suspend fun updatePMFaq(
        @Header("Authorization") token: String,
        @Path("faqId") faqId: String,
        @Body request: PMUpdateFaqRequest
    ): Response<PMFaqItem>

    @DELETE("platform/faq/{faqId}")
    suspend fun deletePMFaq(
        @Header("Authorization") token: String,
        @Path("faqId") faqId: String
    ): Response<Map<String, String>>

    @GET("platform/health")
    suspend fun getPMSystemHealth(
        @Header("Authorization") token: String
    ): Response<PMSystemHealthResponse>

    @POST("platform/health/alerts/{alertId}/resolve")
    suspend fun resolvePMAlert(
        @Header("Authorization") token: String,
        @Path("alertId") alertId: String
    ): Response<Map<String, String>>
}
