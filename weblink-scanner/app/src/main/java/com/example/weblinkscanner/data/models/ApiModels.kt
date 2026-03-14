package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

// -------------------------------------------------
// SUBSCRIPTION PLAN
// -------------------------------------------------

data class PlanInfo(
    val name: String,
    val price: String,

    @SerializedName("scan_limit")
    val scanLimit: String,
    val features: List<String>
)

data class UserPlanResponse(
    val currentPlan: String,
    val scansToday: Int,
    val dailyLimit: Int?,
    val planDetails: PlanInfo
)

data class AllPlansResponse(val plans: List<PlanInfo>)

data class UpgradePlanRequest(
    @SerializedName("new_plan")
    val newPlan: String
)

data class UpgradePlanResponse(
    val message: String,

    @SerializedName("new_plan")
    val newPlan: String
)


// -------------------------------------------------
// MANUAL URL SCAN
// -------------------------------------------------

data class ScanRequest(
    val url: String,
    val source: String = "manual"
)

data class ScanResponse(
    @SerializedName("scan_id")
    val scanId: String,
    val url: String? = null,

    @SerializedName("verdict")
    val riskLevel: String,

    @SerializedName("risk_score")
    val riskScore: Double,

    @SerializedName("threat_categories")
    val threatCategories: List<String>,
    val message: String? = null,

    @SerializedName("scanned_at")
    val scannedAt: String
)


// -------------------------------------------------
// CAMERA OCR SCAN
// -------------------------------------------------

data class CameraScanRequest(
    @SerializedName("extracted_text")
    val extractedText: String
)

data class CameraScanResponse(
    @SerializedName("extracted_url")
    val extractedUrl: String?,

    @SerializedName("is_url")
    val isUrl: Boolean,

    @SerializedName("scan_result")
    val scanResult: ScanResponse?
)


// -------------------------------------------------
// QR SCAN
// -------------------------------------------------

data class QRScanRequest(
    @SerializedName("raw_qr_data")
    val rawQrData: String
)

data class QRScanResponse(
    @SerializedName("extracted_url")
    val extractedUrl: String?,

    @SerializedName("is_url")
    val isUrl: Boolean,

    @SerializedName("scan_result")
    val scanResult: ScanResponse?
)


// -------------------------------------------------
// SANDBOX REPORT
// -------------------------------------------------

data class SandboxRequest(
    val url: String,

    @SerializedName("scan_id")
    val scanId: String
)

data class SslInfo(
    val valid: Boolean?,
    val issuer: String?,
    val expiry: String?
)

data class SandboxReport(
    @SerializedName("sandbox_id")
    val sandboxId: String,
    val url: String,

    @SerializedName("status_code")
    val statusCode: Int?,

    @SerializedName("page_title")
    val pageTitle: String?,

    @SerializedName("ip_address")
    val ipAddress: String?,

    @SerializedName("load_time_ms")
    val loadTimeMs: Int?,

    @SerializedName("redirect_chain")
    val redirectChain: List<String>,

    @SerializedName("external_links")
    val externalLinks: List<String>,

    @SerializedName("ssl_info")
    val sslInfo: SslInfo?,

    //@SerializedName("screenshot_path")
    //val screenshotPath: String?,

    @SerializedName("created_at")
    val createdAt: String,
)
