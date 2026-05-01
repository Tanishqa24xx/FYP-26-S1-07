package com.example.weblinkscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weblinkscanner.data.api.NewRetrofitClient
import com.example.weblinkscanner.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    sealed class UiState<out T> {
        object Idle    : UiState<Nothing>()
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val message: String) : UiState<Nothing>()
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private val _stats = MutableStateFlow<UiState<AdminStatsResponse>>(UiState.Idle)
    val stats: StateFlow<UiState<AdminStatsResponse>> = _stats.asStateFlow()

    // ── Users list ────────────────────────────────────────────────────────────
    private val _users = MutableStateFlow<UiState<List<AdminUser>>>(UiState.Idle)
    val users: StateFlow<UiState<List<AdminUser>>> = _users.asStateFlow()

    // ── Selected user ─────────────────────────────────────────────────────────
    private val _selectedUser = MutableStateFlow<UiState<AdminUser>>(UiState.Idle)
    val selectedUser: StateFlow<UiState<AdminUser>> = _selectedUser.asStateFlow()

    // ── Action result (suspend/reactivate/lock/unlock/create/update) ──────────
    private val _actionResult = MutableStateFlow<UiState<String>>(UiState.Idle)
    val actionResult: StateFlow<UiState<String>> = _actionResult.asStateFlow()

    fun clearActionResult() { _actionResult.value = UiState.Idle }

    // ── Load Stats ────────────────────────────────────────────────────────────
    fun loadStats(token: String) {
        viewModelScope.launch {
            _stats.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getAdminStats("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _stats.value = UiState.Success(response.body()!!)
                } else {
                    _stats.value = UiState.Error("Failed to load stats")
                }
            } catch (e: Exception) {
                _stats.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Load Users ────────────────────────────────────────────────────────────
    fun loadUsers(token: String, search: String? = null) {
        viewModelScope.launch {
            _users.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getAdminUsers("Bearer $token", search)
                if (response.isSuccessful && response.body() != null) {
                    _users.value = UiState.Success(response.body()!!.users)
                } else {
                    _users.value = UiState.Error("Failed to load users")
                }
            } catch (e: Exception) {
                _users.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Load Single User ──────────────────────────────────────────────────────
    fun loadUser(token: String, userId: String) {
        viewModelScope.launch {
            _selectedUser.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getAdminUser("Bearer $token", userId)
                if (response.isSuccessful && response.body() != null) {
                    _selectedUser.value = UiState.Success(response.body()!!)
                } else {
                    _selectedUser.value = UiState.Error("User not found")
                }
            } catch (e: Exception) {
                _selectedUser.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Create User ───────────────────────────────────────────────────────────
    fun createUser(token: String, request: CreateAdminUserRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _actionResult.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.createAdminUser("Bearer $token", request)
                if (response.isSuccessful) {
                    _actionResult.value = UiState.Success("User created successfully")
                    onDone()
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to create user"
                    _actionResult.value = UiState.Error(err)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Update User ───────────────────────────────────────────────────────────
    fun updateUser(token: String, userId: String, request: UpdateAdminUserRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _actionResult.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.updateAdminUser("Bearer $token", userId, request)
                if (response.isSuccessful) {
                    _actionResult.value = UiState.Success("User updated successfully")
                    onDone()
                } else {
                    _actionResult.value = UiState.Error("Failed to update user")
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Account Status Actions ────────────────────────────────────────────────
    private fun performAction(
        token: String,
        userId: String,
        successMsg: String,
        action: suspend () -> retrofit2.Response<Map<String, String>>
    ) {
        viewModelScope.launch {
            _actionResult.value = UiState.Loading
            try {
                val response = action()
                if (response.isSuccessful) {
                    _actionResult.value = UiState.Success(successMsg)
                    loadUser(token, userId)
                } else {
                    _actionResult.value = UiState.Error("Action failed")
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun suspendUser(token: String, userId: String) = performAction(token, userId, "User suspended") {
        NewRetrofitClient.api.suspendUser("Bearer $token", userId)
    }

    fun reactivateUser(token: String, userId: String) = performAction(token, userId, "User reactivated") {
        NewRetrofitClient.api.reactivateUser("Bearer $token", userId)
    }

    fun lockUser(token: String, userId: String) = performAction(token, userId, "User locked") {
        NewRetrofitClient.api.lockUser("Bearer $token", userId)
    }

    fun unlockUser(token: String, userId: String) = performAction(token, userId, "User unlocked") {
        NewRetrofitClient.api.unlockUser("Bearer $token", userId)
    }

    fun approveUser(token: String, userId: String) = performAction(token, userId, "User approved") {
        NewRetrofitClient.api.approveUser("Bearer $token", userId)
    }

    fun rejectUser(token: String, userId: String) = performAction(token, userId, "User rejected") {
        NewRetrofitClient.api.rejectUser("Bearer $token", userId)
    }

    // ── Profile State ─────────────────────────────────────────────────────────
    private val _profiles = MutableStateFlow<UiState<List<UserProfile>>>(UiState.Idle)
    val profiles: StateFlow<UiState<List<UserProfile>>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<UiState<UserProfile>>(UiState.Idle)
    val selectedProfile: StateFlow<UiState<UserProfile>> = _selectedProfile.asStateFlow()

    private val _profileActionResult = MutableStateFlow<UiState<String>>(UiState.Idle)
    val profileActionResult: StateFlow<UiState<String>> = _profileActionResult.asStateFlow()

    fun clearProfileActionResult() { _profileActionResult.value = UiState.Idle }

    // ── Load Profiles ─────────────────────────────────────────────────────────
    fun loadProfiles(token: String, search: String? = null) {
        viewModelScope.launch {
            _profiles.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getProfiles("Bearer $token", search)
                if (response.isSuccessful && response.body() != null) {
                    _profiles.value = UiState.Success(response.body()!!.profiles)
                } else {
                    _profiles.value = UiState.Error("Failed to load profiles")
                }
            } catch (e: Exception) {
                _profiles.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Load Single Profile ───────────────────────────────────────────────────
    fun loadProfile(token: String, profileId: String) {
        viewModelScope.launch {
            _selectedProfile.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getProfile("Bearer $token", profileId)
                if (response.isSuccessful && response.body() != null) {
                    _selectedProfile.value = UiState.Success(response.body()!!)
                } else {
                    _selectedProfile.value = UiState.Error("Profile not found")
                }
            } catch (e: Exception) {
                _selectedProfile.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Create Profile ────────────────────────────────────────────────────────
    fun createProfile(token: String, request: CreateProfileRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _profileActionResult.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.createProfile("Bearer $token", request)
                if (response.isSuccessful) {
                    _profileActionResult.value = UiState.Success("Profile created successfully")
                    onDone()
                } else {
                    val err = try {
                        org.json.JSONObject(response.errorBody()?.string() ?: "").optString("detail", "Failed to create profile")
                    } catch (e: Exception) { "Failed to create profile" }
                    _profileActionResult.value = UiState.Error(err)
                }
            } catch (e: Exception) {
                _profileActionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Update Profile ────────────────────────────────────────────────────────
    fun updateProfile(token: String, profileId: String, request: AdminProfileUpdateRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _profileActionResult.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.updateAdminProfile("Bearer $token", profileId, request)
                if (response.isSuccessful) {
                    _profileActionResult.value = UiState.Success("Profile updated successfully")
                    loadProfile(token, profileId)
                    onDone()
                } else {
                    _profileActionResult.value = UiState.Error("Failed to update profile")
                }
            } catch (e: Exception) {
                _profileActionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Suspend / Reactivate Profile ──────────────────────────────────────────
    private fun performProfileAction(token: String, profileId: String, successMsg: String,
        action: suspend () -> retrofit2.Response<Map<String, String>>) {
        viewModelScope.launch {
            _profileActionResult.value = UiState.Loading
            try {
                val response = action()
                if (response.isSuccessful) {
                    _profileActionResult.value = UiState.Success(successMsg)
                    loadProfile(token, profileId)
                } else {
                    _profileActionResult.value = UiState.Error("Action failed")
                }
            } catch (e: Exception) {
                _profileActionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun suspendProfile(token: String, profileId: String) =
        performProfileAction(token, profileId, "Profile suspended") {
            NewRetrofitClient.api.suspendProfile("Bearer $token", profileId)
        }

    fun reactivateProfile(token: String, profileId: String) =
        performProfileAction(token, profileId, "Profile reactivated") {
            NewRetrofitClient.api.reactivateProfile("Bearer $token", profileId)
        }

    // ── Assign / Remove Profile from User ─────────────────────────────────────
    fun assignProfile(token: String, userId: String, profileId: String) {
        viewModelScope.launch {
            _actionResult.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.assignProfile(
                    "Bearer $token", userId, AssignProfileRequest(profileId)
                )
                if (response.isSuccessful) {
                    val msg = response.body()?.get("message") ?: "Profile assigned"
                    _actionResult.value = UiState.Success(msg)
                    loadUser(token, userId)
                } else {
                    val err = try {
                        org.json.JSONObject(response.errorBody()?.string() ?: "").optString("detail", "Failed to assign profile")
                    } catch (e: Exception) { "Failed to assign profile" }
                    _actionResult.value = UiState.Error(err)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun removeProfile(token: String, userId: String) {
        viewModelScope.launch {
            _actionResult.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.removeProfile("Bearer $token", userId)
                if (response.isSuccessful) {
                    _actionResult.value = UiState.Success("Profile removed from user")
                    loadUser(token, userId)
                } else {
                    _actionResult.value = UiState.Error("Failed to remove profile")
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Security Monitor ──────────────────────────────────────────────────────
    private val _securityUsers = MutableStateFlow<UiState<List<AdminUser>>>(UiState.Idle)
    val securityUsers: StateFlow<UiState<List<AdminUser>>> = _securityUsers.asStateFlow()

    fun loadSecurityUsers(token: String) {
        viewModelScope.launch {
            _securityUsers.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getSecurityUsers("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _securityUsers.value = UiState.Success(response.body()!!.users)
                } else {
                    _securityUsers.value = UiState.Error("Failed to load security data")
                }
            } catch (e: Exception) {
                _securityUsers.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Scan Records ──────────────────────────────────────────────────────────
    private val _scanRecords = MutableStateFlow<UiState<AdminScanRecordsResponse>>(UiState.Idle)
    val scanRecords: StateFlow<UiState<AdminScanRecordsResponse>> = _scanRecords.asStateFlow()

    fun loadScanRecords(token: String, verdict: String? = null) {
        viewModelScope.launch {
            _scanRecords.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getAdminScans("Bearer $token", verdict)
                if (response.isSuccessful && response.body() != null) {
                    _scanRecords.value = UiState.Success(response.body()!!)
                } else {
                    _scanRecords.value = UiState.Error("Failed to load scan records")
                }
            } catch (e: Exception) {
                _scanRecords.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Flagged Links ─────────────────────────────────────────────────────────
    private val _flaggedLinks = MutableStateFlow<UiState<AdminScanRecordsResponse>>(UiState.Idle)
    val flaggedLinks: StateFlow<UiState<AdminScanRecordsResponse>> = _flaggedLinks.asStateFlow()

    fun loadFlaggedLinks(token: String, verdict: String? = null) {
        viewModelScope.launch {
            _flaggedLinks.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getFlaggedLinks("Bearer $token", verdict)
                if (response.isSuccessful && response.body() != null) {
                    _flaggedLinks.value = UiState.Success(response.body()!!)
                } else {
                    _flaggedLinks.value = UiState.Error("Failed to load flagged links")
                }
            } catch (e: Exception) {
                _flaggedLinks.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Audit Log ─────────────────────────────────────────────────────────────
    private val _auditLog = MutableStateFlow<UiState<List<AuditLogEntry>>>(UiState.Idle)
    val auditLog: StateFlow<UiState<List<AuditLogEntry>>> = _auditLog.asStateFlow()

    fun loadAuditLog(token: String) {
        viewModelScope.launch {
            _auditLog.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getAuditLog("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _auditLog.value = UiState.Success(response.body()!!.entries)
                } else {
                    _auditLog.value = UiState.Error("Failed to load audit log")
                }
            } catch (e: Exception) {
                _auditLog.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────
    private val _subscriptions = MutableStateFlow<UiState<SubscriptionResponse>>(UiState.Idle)
    val subscriptions: StateFlow<UiState<SubscriptionResponse>> = _subscriptions.asStateFlow()

    fun loadSubscriptions(token: String) {
        viewModelScope.launch {
            _subscriptions.value = UiState.Loading
            try {
                val response = NewRetrofitClient.api.getSubscriptions("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _subscriptions.value = UiState.Success(response.body()!!)
                } else {
                    _subscriptions.value = UiState.Error("Failed to load subscriptions")
                }
            } catch (e: Exception) {
                _subscriptions.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    fun loadPendingCount(token: String) {
        viewModelScope.launch {
            try {
                val r = NewRetrofitClient.api.getAdminPendingCount("Bearer $token")
                if (r.isSuccessful) _pendingCount.value = r.body()?.get("count")?.toInt() ?: 0
            } catch (e: Exception) { /* silent */ }
        }
    }
}
