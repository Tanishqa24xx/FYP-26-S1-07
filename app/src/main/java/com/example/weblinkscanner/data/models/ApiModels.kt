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
    @SerializedName("valid_from") val validFrom: String?,
    @SerializedName("valid_days") val validDays: String?,
    @SerializedName("age_days") val ageDays: String?,
    val expiry: String? = null,
    val protocol: String?
)

data class AsnInfo(
    val asn: String?,
    val asnname: String?,
    val country: String?
)

data class SandboxReport(
    @SerializedName("sandbox_id") val sandboxId: String,
    val url: String,
    @SerializedName("created_at") val createdAt: String,

    // Page overview
    @SerializedName("status_code") val statusCode: Int?,
    @SerializedName("page_title") val pageTitle: String?,
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("load_time_ms") val loadTimeMs: Int?,
    @SerializedName("final_url") val finalUrl: String?,
    val server: String?,
    @SerializedName("mime_type") val mimeType: String?,
    val ptr: String?,
    val country: String?,
    val city: String?,
    @SerializedName("apex_domain") val apexDomain: String?,

    // SSL
    @SerializedName("ssl_info") val sslInfo: SslInfo?,

    // Network
    @SerializedName("redirect_chain") val redirectChain: List<String> = emptyList(),
    @SerializedName("external_links") val externalLinks: List<String> = emptyList(),
    @SerializedName("domains_contacted") val domainsContacted: List<String> = emptyList(),
    @SerializedName("domain_count") val domainCount: Int?,
    @SerializedName("ips_contacted") val ipsContacted: List<String> = emptyList(),
    @SerializedName("ip_count") val ipCount: Int?,
    @SerializedName("urls_contacted") val urlsContacted: List<String> = emptyList(),

    // Content
    @SerializedName("tech_detected") val techDetected: List<String> = emptyList(),
    @SerializedName("console_messages") val consoleMessages: List<String> = emptyList(),
    @SerializedName("total_size_kb") val totalSizeKb: Int?,
    @SerializedName("total_requests") val totalRequests: Int?,

    // urlscan verdict
    @SerializedName("verdict_score") val verdictScore: Int?,
    @SerializedName("verdict_categories") val verdictCategories: List<String> = emptyList(),
    val malicious: Boolean?,

    // Hosting
    @SerializedName("asn_info") val asnInfo: AsnInfo?,

    // Screenshot and report links
    @SerializedName("screenshot_url") val screenshotUrl: String?,
    @SerializedName("screenshot_b64") val screenshotB64: String?,
    @SerializedName("report_url") val reportUrl: String?,
    @SerializedName("sandbox_uuid") val sandboxUuid: String?,

    @SerializedName("analysis_source") val analysisSource: String?,

    // Premium enrichment — ad/tracker/script analysis
    @SerializedName("detected_ad_tech") val detectedAdTech: List<String> = emptyList(),
    @SerializedName("detected_trackers") val detectedTrackers: List<String> = emptyList(),
    @SerializedName("suspicious_scripts") val suspiciousScripts: List<String> = emptyList(),
    @SerializedName("ad_heavy") val adHeavy: Boolean = false
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


// --- SAVED LINKS RECHECK ---

data class RecheckUrlItem(
    val id: String,
    val url: String
)

data class RecheckRequest(
    @SerializedName("user_id") val userId: String,
    val links: List<RecheckUrlItem>
)

data class RecheckResultItem(
    val id: String,
    val url: String,
    @SerializedName("new_risk_level") val newRiskLevel: String,
    @SerializedName("last_checked_at") val lastCheckedAt: String
)

data class RecheckResponse(
    val results: List<RecheckResultItem>,
    val errors: List<String> = emptyList()
)


// --- Rescan ---

data class RescanResponse(
    val message: String,
    @SerializedName("quota_warning") val quotaWarning: Boolean = false,
    val remaining: Int = 0,
    val total: Int = 0,
    val scanned: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0
)

// --- Update Profile ---

data class UpdateProfileRequest(
    @SerializedName("user_id") val userId: String,
    val name: String,
    val email: String
)

// ── USER SUPPORT / REPORT ─────────────────────────────────────────────────────

data class CreateSupportRequest(
    @SerializedName("user_id") val userId:  String,
    val email:   String,
    val subject: String,
    val message: String
)

data class SupportReply(
    val id:           String,
    @SerializedName("request_id")   val requestId:  String,
    val message:      String,
    @SerializedName("sender_type")  val senderType: String,
    @SerializedName("sender_email") val senderEmail: String?,
    @SerializedName("created_at")   val createdAt:  String?
)

data class UserSupportRequest(
    val id:         String,
    @SerializedName("user_id")    val userId:    String,
    val email:      String?,
    val subject:    String,
    val message:    String,
    val status:     String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val replies:    List<SupportReply> = emptyList()
)

data class UserSupportListResponse(
    val requests: List<UserSupportRequest>
)
