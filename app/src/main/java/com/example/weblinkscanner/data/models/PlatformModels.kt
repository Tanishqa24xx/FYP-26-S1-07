package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

// ── Subscription Plans ────────────────────────────────────────────────────────

data class PMSubscriptionPlan(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    @SerializedName("scan_limit")  val scanLimit: Int,
    val features: List<String>,
    val status: String,
    @SerializedName("user_count")  val userCount: Int? = 0,
    @SerializedName("created_at")  val createdAt: String?    = null,
    val users: List<PMPlanUser>?   = null
)

data class PMPlanUser(val id: String, val name: String?, val email: String?)

data class PMPlansResponse(val plans: List<PMSubscriptionPlan>)

data class PMCreatePlanRequest(
    val name: String,
    val description: String = "",
    val price: Double = 0.0,
    @SerializedName("scan_limit") val scanLimit: Int = 10,
    val features: List<String> = emptyList()
)

data class PMUpdatePlanRequest(
    val name: String?        = null,
    val description: String? = null,
    val price: Double?       = null,
    @SerializedName("scan_limit") val scanLimit: Int? = null,
    val features: List<String>?   = null
)

// ── Analytics ─────────────────────────────────────────────────────────────────

data class PMAnalyticsOverview(
    @SerializedName("total_users")      val totalUsers: Int,
    @SerializedName("active_users")     val activeUsers: Int,
    @SerializedName("total_scans")      val totalScans: Int,
    @SerializedName("scans_today")      val scansToday: Int,
    @SerializedName("scans_this_month") val scansThisMonth: Int,
    @SerializedName("new_users_today")  val newUsersToday: Int,
    @SerializedName("plan_distribution") val planDistribution: Map<String, Int>
)

data class PMFeatureAnalytics(
    @SerializedName("verdict_breakdown") val verdictBreakdown: Map<String, Int>,
    @SerializedName("plan_distribution") val planDistribution: Map<String, Int>,
    @SerializedName("feature_usage")     val featureUsage: Map<String, Int>
)

data class PMReportResponse(
    val period: Map<String, String?>,
    @SerializedName("total_scans")       val totalScans: Int,
    @SerializedName("new_users")         val newUsers: Int,
    @SerializedName("scans_by_date")     val scansByDate: Map<String, Int>,
    @SerializedName("verdict_breakdown") val verdictBreakdown: Map<String, Int>,
    @SerializedName("new_users_by_plan") val newUsersByPlan: Map<String, Int>
)

// ── Support Requests ──────────────────────────────────────────────────────────

data class PMSupportRequest(
    val id: String,
    @SerializedName("user_id")    val userId: String?,
    @SerializedName("user_email") val userEmail: String?,
    val subject: String,
    val message: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String?,
    val replies: List<PMSupportReply>? = null
)

data class PMSupportReply(
    val id: String,
    @SerializedName("request_id")  val requestId: String,
    val message: String,
    @SerializedName("sender_type") val senderType: String,
    @SerializedName("sender_email") val senderEmail: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class PMSupportRequestsResponse(val requests: List<PMSupportRequest>)

data class PMCreateReplyRequest(
    val message: String,
    @SerializedName("sender_email") val senderEmail: String? = null
)

data class PMUpdateSupportStatusRequest(val status: String)

// ── FAQ ───────────────────────────────────────────────────────────────────────

data class PMFaqItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: String?,
    @SerializedName("sort_order") val sortOrder: Int?,
    @SerializedName("is_active")  val isActive: Boolean?,
    @SerializedName("created_at") val createdAt: String? = null
)

data class PMFaqResponse(val faqs: List<PMFaqItem>)

data class PMCreateFaqRequest(
    val question: String,
    val answer: String,
    val category: String = "General",
    @SerializedName("sort_order") val sortOrder: Int = 0
)

data class PMUpdateFaqRequest(
    val question: String?  = null,
    val answer: String?    = null,
    val category: String?  = null,
    @SerializedName("sort_order") val sortOrder: Int? = null,
    @SerializedName("is_active")  val isActive: Boolean? = null
)

// ── System Health ─────────────────────────────────────────────────────────────

data class PMServiceStatus(
    val status: String,
    @SerializedName("response_ms") val responseMs: Int?
)

data class PMSystemAlert(
    val id: String,
    val type: String,
    val message: String,
    val severity: String,
    val resolved: Boolean,
    @SerializedName("created_at") val createdAt: String?
)

data class PMSystemHealthResponse(
    @SerializedName("overall_status") val overallStatus: String,
    val services: Map<String, PMServiceStatus>,
    @SerializedName("active_alerts") val activeAlerts: List<PMSystemAlert>,
    @SerializedName("alert_count")   val alertCount: Int
)
