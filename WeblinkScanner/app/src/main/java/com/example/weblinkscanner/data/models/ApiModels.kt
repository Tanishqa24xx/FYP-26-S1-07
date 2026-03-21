package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

// --- SCAN ---

data class NewScanRequest(
    val url: String,
    val source: String = "manual",
    @SerializedName("user_id") val userId: String = "00000000-0000-0000-0000-000000000000"
)

data class NewScanResponse(
    @SerializedName("scan_id") val scanId: String,
    val url: String? = null,
    @SerializedName("verdict") val riskLevel: String,
    @SerializedName("risk_score") val riskScore: Double,
    @SerializedName("threat_categories") val threatCategories: List<String>,
    val message: String? = null,
    @SerializedName("scanned_at") val scannedAt: String,
    @SerializedName("scans_remaining") val scansRemaining: Int? = null
)

// --- CAMERA ---

data class CameraScanRequest(
    @SerializedName("extracted_text") val extractedText: String,
    @SerializedName("user_id") val userId: String = "00000000-0000-0000-0000-000000000000"
)

data class CameraScanResponse(
    @SerializedName("extracted_url") val extractedUrl: String?,
    @SerializedName("is_url") val isUrl: Boolean,
    @SerializedName("scan_result") val scanResult: NewScanResponse?
)

// --- QR ---

data class QRScanRequest(
    @SerializedName("raw_qr_data") val rawQrData: String,
    @SerializedName("user_id") val userId: String = "00000000-0000-0000-0000-000000000000"
)

data class QRScanResponse(
    @SerializedName("extracted_url") val extractedUrl: String?,
    @SerializedName("is_url") val isUrl: Boolean,
    @SerializedName("scan_result") val scanResult: NewScanResponse?
)

// --- SANDBOX ---

data class SandboxRequest(
    val url: String,
    @SerializedName("scan_id") val scanId: String
)

data class SslInfo(
    val valid: Boolean?,
    val issuer: String?,
    val expiry: String?
)

data class SandboxReport(
    @SerializedName("sandbox_id") val sandboxId: String,
    val url: String,
    @SerializedName("status_code") val statusCode: Int?,
    @SerializedName("page_title") val pageTitle: String?,
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("load_time_ms") val loadTimeMs: Int?,
    @SerializedName("redirect_chain") val redirectChain: List<String>,
    @SerializedName("external_links") val externalLinks: List<String>,
    @SerializedName("ssl_info") val sslInfo: SslInfo?,
    @SerializedName("created_at") val createdAt: String
)

// --- PLANS ---

data class PlanInfo(
    val name: String,
    val price: String,
    @SerializedName("scan_limit") val scanLimit: String,
    val features: List<String>
)

data class UserPlanResponse(
    val currentPlan: String,
    val scansToday: Int,
    val dailyLimit: Int?,
    val planDetails: PlanInfo
)

data class AllPlansResponse(val plans: List<PlanInfo>)

data class UpgradePlanRequest(@SerializedName("new_plan") val newPlan: String)

data class UpgradePlanResponse(
    val message: String,
    @SerializedName("new_plan") val newPlan: String
)

// --- SAVED LINKS ---

data class SaveLinkRequest(
    @SerializedName("user_id") val userId: String,
    val url: String,
    @SerializedName("scan_id") val scanId: String? = null,
    @SerializedName("risk_level") val riskLevel: String? = null
)

data class SavedLinkItem(
    val id: String,
    val url: String,
    @SerializedName("risk_level") val riskLevel: String?,
    @SerializedName("last_checked_at") val lastCheckedAt: String?
)

data class SavedLinksResponse(val links: List<SavedLinkItem>)
