// app/src/main/java/com/example/weblinkscanner/data/repository/WeblinkScannerRepository.kt

package com.example.weblinkscanner.data.repository

import com.example.weblinkscanner.data.api.RetrofitClient
import com.example.weblinkscanner.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Response

// ----- Simple result wrapper -----
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
}

class WeblinkScannerRepository(private val session: SessionStore) {

    private val api = RetrofitClient.api

    // Helper: reads token from DataStore and formats as Bearer
    private suspend fun bearer(): String {
        val token = session.accessToken.first() ?: ""
        return "Bearer $token"
    }

    // ----- Subscription plans -----
    suspend fun getMyPlan(): Result<UserPlanResponse> = safeCall { api.getMyPlan(bearer()) }

    suspend fun getAllPlans(): Result<AllPlansResponse> = safeCall { api.getAllPlans() }

    suspend fun upgradePlan(newPlan: String): Result<UpgradePlanResponse> =
        safeCall { api.upgradePlan(bearer(), UpgradePlanRequest(newPlan)) }

    // ----- URL scanning -----
    suspend fun scanUrl(url: String): Result<ScanResponse> =
        safeCall { api.scanUrl(bearer(), ScanRequest(url, "manual")) }

    suspend fun scanCamera(extractedText: String): Result<CameraScanResponse> =
        safeCall { api.scanCamera(bearer(), CameraScanRequest(extractedText)) }

    suspend fun scanQr(rawQrData: String): Result<QRScanResponse> =
        safeCall { api.scanQr(bearer(), QRScanRequest(rawQrData)) }

    // ----- Sandbox analysis -----
    suspend fun analyseSandbox(url: String, scanId: String): Result<SandboxReport> =
        safeCall { api.analyseSandbox(bearer(), SandboxRequest(url, scanId)) }

    suspend fun getSandboxReport(scanId: String): Result<SandboxReport> =
        safeCall { api.getSandboxReport(bearer(), scanId) }

    // ----- Generic safe call wrapper -----
    private suspend fun <T> safeCall(call: suspend () -> Response<T>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) Result.Success(body)
                    else Result.Error("Empty response", response.code())
                } else {
                    val errorText = response.errorBody()?.string() ?: "Unknown error"
                    Result.Error(errorText, response.code())
                }
            } catch (e: Exception) {
                Result.Error(e.localizedMessage ?: "Network error")
            }
        }
}
