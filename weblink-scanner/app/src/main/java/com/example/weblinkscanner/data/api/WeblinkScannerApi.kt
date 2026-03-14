package com.example.weblinkscanner.data.api

import com.example.weblinkscanner.data.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface WeblinkScannerApi {
    // ------------------------------------------------
    // SUBSCRIPTION PLAN
    // ------------------------------------------------
    @GET("plan/")
    suspend fun getMyPlan(@Header("Authorization") token: String): Response<UserPlanResponse>

    @GET("plan/all")
    suspend fun getAllPlans(): Response<AllPlansResponse>

    @POST("plan/upgrade")
    suspend fun upgradePlan(
        @Header("Authorization") token: String,
        @Body request: UpgradePlanRequest
    ): Response<UpgradePlanResponse>


    // ------------------------------------------------
    // URL SCANNING
    // ------------------------------------------------

    @POST("scan/url")
    suspend fun scanUrl(
        @Header("Authorization") token: String,
        @Body request: ScanRequest
    ): Response<ScanResponse>


    // ------------------------------------------------
    // CAMERA OCR SCAN
    // ------------------------------------------------

    @POST("scan/camera")
    suspend fun scanCamera(
        @Header("Authorization") token: String,
        @Body request: CameraScanRequest
    ): Response<CameraScanResponse>


    // ------------------------------------------------
    // QR SCAN
    // ------------------------------------------------

    @POST("scan/qr")
    suspend fun scanQr(
        @Header("Authorization") token: String,
        @Body request: QRScanRequest
    ): Response<QRScanResponse>


    // ------------------------------------------------
    // SANDBOX ANALYSIS
    // ------------------------------------------------

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
}
