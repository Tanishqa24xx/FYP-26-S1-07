
/*
 This repository handles all our API calls to the FastAPI backend.
 It's basically the middleman between the Retrofit client and our ViewModels.
 Added some safeCall logic here to catch network errors and keep the app from crashing.
*/

package com.example.weblinkscanner.data.repository

import com.example.weblinkscanner.data.api.NewRetrofitClient
import com.example.weblinkscanner.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
}

class WeblinkScannerRepository(private val session: SessionStore) {

    private val api = NewRetrofitClient.api

    private suspend fun bearer(): String {
        val token = session.accessToken.first() ?: ""
        return "Bearer $token"
    }

    // --- Plans ---
    suspend fun getMyPlan(userId: String): Result<UserPlanResponse> =
        safeCall { api.getMyPlan(bearer(), userId) }

    suspend fun getAllPlans(): Result<AllPlansResponse> =
        safeCall { api.getAllPlans() }

    suspend fun upgradePlan(newPlan: String, userId: String): Result<UpgradePlanResponse> =
        safeCall { api.upgradePlan(bearer(), userId, UpgradePlanRequest(newPlan)) }

    // --- Scanning ---
    suspend fun scanUrl(url: String, userId: String): Result<NewScanResponse> =
        safeCall { api.scanUrl(bearer(), NewScanRequest(url, "manual", userId)) }

    suspend fun scanCamera(extractedText: String, userId: String): Result<CameraScanResponse> =
        safeCall { api.scanCamera(bearer(), CameraScanRequest(extractedText, userId)) }

    suspend fun scanQr(rawQrData: String, userId: String): Result<QRScanResponse> =
        safeCall { api.scanQr(bearer(), QRScanRequest(rawQrData, userId)) }

    // --- Sandbox ---
    suspend fun analyseSandbox(url: String, scanId: String, userId: String): Result<SandboxReport> =
        safeCall { api.analyseSandbox(bearer(), userId, SandboxRequest(url, scanId, userId)) }

    // --- Saved Links ---
    suspend fun saveLink(userId: String, url: String, scanId: String?, riskLevel: String?): Result<Map<String, String>> =
        safeCall { api.saveLink(bearer(), SaveLinkRequest(userId, url, scanId, riskLevel)) }

    suspend fun getSavedLinks(userId: String): Result<SavedLinksResponse> =
        safeCall { api.getSavedLinks(bearer(), userId) }

    suspend fun deleteLinks(ids: List<String>): Result<Map<String, String>> =
        safeCall { api.deleteLinks(bearer(), ids) }

    suspend fun recheckSavedLinks(userId: String, links: List<RecheckUrlItem>): Result<RecheckResponse> =
        safeCall { api.recheckSavedLinks(bearer(), RecheckRequest(userId, links)) }

    // --- Scan History ---
    suspend fun getScanHistory(userId: String): Result<List<NewScanResponse>> =
        safeCall { api.getScanHistory(bearer(), userId) }

    suspend fun deleteHistoryItems(ids: List<String>): Result<Map<String, String>> =
        safeCall { api.deleteHistoryItems(bearer(), ids) }

    suspend fun deleteAccount(userId: String): Result<Map<String, String>> =
        safeCall { api.deleteAccount(bearer(), userId) }

    suspend fun getHelpFaqs(): List<com.example.weblinkscanner.ui.screens.FaqItem> {
        return try {
            val response = api.getFaqs(bearer())
            if (response.isSuccessful) {
                response.body()?.mapNotNull { map ->
                    com.example.weblinkscanner.ui.screens.FaqItem(
                        question = map["question"] ?: "",
                        answer = map["answer"]   ?: "",
                        category = map["category"] ?: ""
                    )
                } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // --- Rescan saved links (quota-aware) ---
    suspend fun rescanSavedLinks(userId: String, force: Boolean = false, selectedIds: List<String> = emptyList()): Result<RescanResponse> =
        safeCall { api.rescanSavedLinks(bearer(), userId, force, selectedIds) }

    // --- Export scan history ---
    suspend fun exportScanHistory(userId: String, fmt: String): Result<okhttp3.ResponseBody> =
        safeCall { api.exportScanHistory(bearer(), userId, fmt) }

    // --- Update profile ---
    suspend fun updateProfile(userId: String, name: String, email: String): Result<Map<String, String>> =
        safeCall { api.updateProfile(bearer(), UpdateProfileRequest(userId, name, email)) }

    // --- User Support ---
    suspend fun submitSupportRequest(userId: String, email: String, subject: String, message: String): Result<Map<String, String>> =
        safeCall { api.submitSupportRequest(bearer(), CreateSupportRequest(userId, email, subject, message)) }

    suspend fun getMySupportRequests(userId: String): Result<UserSupportListResponse> =
        safeCall { api.getMySupportRequests(bearer(), userId) }

    suspend fun userReplySupport(requestId: String, message: String, email: String): Result<Map<String, String>> =
        safeCall { api.userReplySupport(bearer(), requestId, mapOf("message" to message, "sender_email" to email)) }

    // --- Generic safe call ---
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
