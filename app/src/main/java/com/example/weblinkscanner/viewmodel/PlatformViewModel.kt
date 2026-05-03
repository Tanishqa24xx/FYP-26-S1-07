package com.example.weblinkscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weblinkscanner.data.api.NewRetrofitClient
import com.example.weblinkscanner.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlatformViewModel : ViewModel() {

    sealed class UiState<out T> {
        object Idle    : UiState<Nothing>()
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val message: String) : UiState<Nothing>()
    }

    // --- Plans ---
    private val _plans = MutableStateFlow<UiState<List<PMSubscriptionPlan>>>(UiState.Idle)
    val plans: StateFlow<UiState<List<PMSubscriptionPlan>>> = _plans.asStateFlow()

    private val _selectedPlan = MutableStateFlow<UiState<PMSubscriptionPlan>>(UiState.Idle)
    val selectedPlan: StateFlow<UiState<PMSubscriptionPlan>> = _selectedPlan.asStateFlow()

    private val _planAction = MutableStateFlow<UiState<String>>(UiState.Idle)
    val planAction: StateFlow<UiState<String>> = _planAction.asStateFlow()
    fun clearPlanAction() { _planAction.value = UiState.Idle }

    fun loadPlans(token: String) {
        viewModelScope.launch {
            _plans.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMPlans("Bearer $token")
                _plans.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!.plans) else UiState.Error("Failed to load plans")
            } catch (e: Exception) { _plans.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun loadPlan(token: String, planId: String) {
        viewModelScope.launch {
            _selectedPlan.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMPlan("Bearer $token", planId)
                _selectedPlan.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!) else UiState.Error("Plan not found")
            } catch (e: Exception) { _selectedPlan.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun createPlan(token: String, request: PMCreatePlanRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _planAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.createPMPlan("Bearer $token", request)
                if (r.isSuccessful) { _planAction.value = UiState.Success("Plan created"); onDone() }
                else _planAction.value = UiState.Error(r.errorBody()?.string() ?: "Failed")
            } catch (e: Exception) { _planAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun updatePlan(token: String, planId: String, request: PMUpdatePlanRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _planAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.updatePMPlan("Bearer $token", planId, request)
                if (r.isSuccessful) { _planAction.value = UiState.Success("Plan updated"); loadPlan(token, planId); onDone() }
                else _planAction.value = UiState.Error("Failed to update plan")
            } catch (e: Exception) { _planAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    private fun planStatusAction(token: String, planId: String, msg: String,
        action: suspend () -> retrofit2.Response<Map<String, String>>) {
        viewModelScope.launch {
            _planAction.value = UiState.Loading
            try {
                val r = action()
                if (r.isSuccessful) { _planAction.value = UiState.Success(msg); loadPlan(token, planId) }
                else _planAction.value = UiState.Error("Action failed")
            } catch (e: Exception) { _planAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun suspendPlan(token: String, planId: String) = planStatusAction(token, planId, "Plan suspended") {
        NewRetrofitClient.api.suspendPMPlan("Bearer $token", planId) }
    fun activatePlan(token: String, planId: String) = planStatusAction(token, planId, "Plan activated") {
        NewRetrofitClient.api.activatePMPlan("Bearer $token", planId) }

    // --- Analytics ---
    private val _analyticsOverview = MutableStateFlow<UiState<PMAnalyticsOverview>>(UiState.Idle)
    val analyticsOverview: StateFlow<UiState<PMAnalyticsOverview>> = _analyticsOverview.asStateFlow()

    private val _featureAnalytics = MutableStateFlow<UiState<PMFeatureAnalytics>>(UiState.Idle)
    val featureAnalytics: StateFlow<UiState<PMFeatureAnalytics>> = _featureAnalytics.asStateFlow()

    private val _reportData = MutableStateFlow<UiState<PMReportResponse>>(UiState.Idle)
    val reportData: StateFlow<UiState<PMReportResponse>> = _reportData.asStateFlow()
    fun clearReport() { _reportData.value = UiState.Idle }

    fun loadAnalyticsOverview(token: String) {
        viewModelScope.launch {
            _analyticsOverview.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMAnalyticsOverview("Bearer $token")
                _analyticsOverview.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!) else UiState.Error("Failed to load analytics")
            } catch (e: Exception) { _analyticsOverview.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun loadFeatureAnalytics(token: String) {
        viewModelScope.launch {
            _featureAnalytics.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMFeatureAnalytics("Bearer $token")
                _featureAnalytics.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!) else UiState.Error("Failed to load feature analytics")
            } catch (e: Exception) { _featureAnalytics.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun generateReport(token: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _reportData.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.generatePMReport("Bearer $token", startDate, endDate)
                _reportData.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!) else UiState.Error("Failed to generate report")
            } catch (e: Exception) { _reportData.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    // --- Support ---
    private val _supportRequests = MutableStateFlow<UiState<List<PMSupportRequest>>>(UiState.Idle)
    val supportRequests: StateFlow<UiState<List<PMSupportRequest>>> = _supportRequests.asStateFlow()

    private val _selectedSupport = MutableStateFlow<UiState<PMSupportRequest>>(UiState.Idle)
    val selectedSupport: StateFlow<UiState<PMSupportRequest>> = _selectedSupport.asStateFlow()

    private val _supportAction = MutableStateFlow<UiState<String>>(UiState.Idle)
    val supportAction: StateFlow<UiState<String>> = _supportAction.asStateFlow()
    fun clearSupportAction() { _supportAction.value = UiState.Idle }

    fun loadSupportRequests(token: String, status: String? = null) {
        viewModelScope.launch {
            _supportRequests.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMSupportRequests("Bearer $token", status)
                _supportRequests.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!.requests) else UiState.Error("Failed to load support requests")
            } catch (e: Exception) { _supportRequests.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun loadSupportRequest(token: String, requestId: String) {
        viewModelScope.launch {
            _selectedSupport.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMSupportRequest("Bearer $token", requestId)
                _selectedSupport.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!) else UiState.Error("Request not found")
            } catch (e: Exception) { _selectedSupport.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun replyToSupport(token: String, requestId: String, message: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _supportAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.replyToPMSupport("Bearer $token", requestId, PMCreateReplyRequest(message))
                if (r.isSuccessful) { _supportAction.value = UiState.Success("Reply sent"); loadSupportRequest(token, requestId); onDone() }
                else _supportAction.value = UiState.Error("Failed to send reply")
            } catch (e: Exception) { _supportAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun updateSupportStatus(token: String, requestId: String, status: String) {
        viewModelScope.launch {
            _supportAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.updatePMSupportStatus("Bearer $token", requestId, PMUpdateSupportStatusRequest(status))
                if (r.isSuccessful) { _supportAction.value = UiState.Success("Status updated to $status"); loadSupportRequest(token, requestId) }
                else _supportAction.value = UiState.Error("Failed to update status")
            } catch (e: Exception) { _supportAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    // --- FAQ ---
    private val _faqs = MutableStateFlow<UiState<List<PMFaqItem>>>(UiState.Idle)
    val faqs: StateFlow<UiState<List<PMFaqItem>>> = _faqs.asStateFlow()

    private val _faqAction = MutableStateFlow<UiState<String>>(UiState.Idle)
    val faqAction: StateFlow<UiState<String>> = _faqAction.asStateFlow()
    fun clearFaqAction() { _faqAction.value = UiState.Idle }

    fun loadFaqs(token: String) {
        viewModelScope.launch {
            _faqs.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMFaqs("Bearer $token")
                _faqs.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!.faqs) else UiState.Error("Failed to load FAQs")
            } catch (e: Exception) { _faqs.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun createFaq(token: String, request: PMCreateFaqRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _faqAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.createPMFaq("Bearer $token", request)
                if (r.isSuccessful) { _faqAction.value = UiState.Success("FAQ created"); loadFaqs(token); onDone() }
                else _faqAction.value = UiState.Error("Failed to create FAQ")
            } catch (e: Exception) { _faqAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun updateFaq(token: String, faqId: String, request: PMUpdateFaqRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _faqAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.updatePMFaq("Bearer $token", faqId, request)
                if (r.isSuccessful) { _faqAction.value = UiState.Success("FAQ updated"); loadFaqs(token); onDone() }
                else _faqAction.value = UiState.Error("Failed to update FAQ")
            } catch (e: Exception) { _faqAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun deleteFaq(token: String, faqId: String) {
        viewModelScope.launch {
            _faqAction.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.deletePMFaq("Bearer $token", faqId)
                if (r.isSuccessful) { _faqAction.value = UiState.Success("FAQ deleted"); loadFaqs(token) }
                else _faqAction.value = UiState.Error("Failed to delete FAQ")
            } catch (e: Exception) { _faqAction.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    // --- System Health ---
    private val _systemHealth = MutableStateFlow<UiState<PMSystemHealthResponse>>(UiState.Idle)
    val systemHealth: StateFlow<UiState<PMSystemHealthResponse>> = _systemHealth.asStateFlow()

    fun loadSystemHealth(token: String) {
        viewModelScope.launch {
            _systemHealth.value = UiState.Loading
            try {
                val r = NewRetrofitClient.api.getPMSystemHealth("Bearer $token")
                _systemHealth.value = if (r.isSuccessful && r.body() != null)
                    UiState.Success(r.body()!!) else UiState.Error("Failed to load health data")
            } catch (e: Exception) { _systemHealth.value = UiState.Error(e.message ?: "Network error") }
        }
    }

    fun resolveAlert(token: String, alertId: String) {
        viewModelScope.launch {
            try {
                NewRetrofitClient.api.resolvePMAlert("Bearer $token", alertId)
                loadSystemHealth(token)
            } catch (e: Exception) { /* silent fail */ }
        }
    }
}
