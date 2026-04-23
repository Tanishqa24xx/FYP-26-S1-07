package com.example.weblinkscanner.data.models

import com.google.gson.annotations.SerializedName

// ── Admin Stats ───────────────────────────────────────────────────────────────

data class AdminStatsResponse(
    @SerializedName("total_users")   val totalUsers: Int,
    @SerializedName("scans_today")   val scansToday: Int,
    @SerializedName("flagged_links") val flaggedLinks: Int,
    @SerializedName("paid_users")    val paidUsers: Int
)

// ── Admin User ────────────────────────────────────────────────────────────────

data class AdminUser(
    val id: String,
    val name: String?,
    val email: String?,
    val role: String?,
    val plan: String?,
    val status: String?,
    @SerializedName("account_status")     val accountStatus: String?,
    @SerializedName("failed_login_count") val failedLoginCount: Int?,
    @SerializedName("profile_id")         val profileId: String?   = null,
    @SerializedName("profile_name")       val profileName: String? = null
)

data class AdminUsersResponse(
    val users: List<AdminUser>
)

// ── Create / Update User ──────────────────────────────────────────────────────

data class CreateAdminUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user",
    val plan: String = "free"
)

data class UpdateAdminUserRequest(
    val name: String? = null,
    val role: String? = null,
    val plan: String? = null
)

// ── User Profile ──────────────────────────────────────────────────────────────

data class UserProfile(
    val id: String,
    val name: String,
    val description: String?,
    val permissions: List<String>,
    val status: String,
    @SerializedName("created_at")    val createdAt: String?,
    @SerializedName("assigned_users") val assignedUsers: List<ProfileAssignedUser>?
)

data class ProfileAssignedUser(
    val id: String,
    val name: String?,
    val email: String?,
    val role: String?
)

data class UserProfilesResponse(val profiles: List<UserProfile>)

data class CreateProfileRequest(
    val name: String,
    val description: String = "",
    val permissions: List<String> = emptyList()
)

data class AdminProfileUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val permissions: List<String>? = null
)

data class AssignProfileRequest(
    @SerializedName("profile_id") val profileId: String
)

// ── Admin Scan Record ─────────────────────────────────────────────────────────

data class AdminScanRecord(
    val id: String,
    val url: String?,
    val verdict: String?,
    @SerializedName("risk_score")        val riskScore: Double?,
    @SerializedName("threat_categories") val threatCategories: List<String>?,
    @SerializedName("scanned_at")        val scannedAt: String?,
    @SerializedName("user_id")           val userId: String?,
    @SerializedName("user_email")        val userEmail: String?
)

data class AdminScanRecordsResponse(
    val records: List<AdminScanRecord>,
    val total: Int
)

// ── Audit Log ─────────────────────────────────────────────────────────────────

data class AuditLogEntry(
    val id: String,
    val action: String,
    @SerializedName("target_type")  val targetType: String?,
    @SerializedName("target_id")    val targetId: String?,
    @SerializedName("target_email") val targetEmail: String?,
    val details: String?,
    @SerializedName("admin_email")  val adminEmail: String?,
    @SerializedName("created_at")   val createdAt: String
)

data class AuditLogResponse(val entries: List<AuditLogEntry>)

// ── Subscription Management ───────────────────────────────────────────────────

data class SubscriptionStats(
    val total: Int,
    val free: Int,
    val standard: Int,
    val premium: Int
)

data class SubscriptionUser(
    val id: String,
    val name: String?,
    val email: String?,
    val plan: String?
)

data class SubscriptionResponse(
    val stats: SubscriptionStats,
    val users: List<SubscriptionUser>
)
