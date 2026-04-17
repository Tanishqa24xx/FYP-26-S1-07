package com.example.weblinkscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weblinkscanner.data.models.PlanInfo
import com.example.weblinkscanner.data.models.UserPlanResponse
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlanViewModel(private val repository: WeblinkScannerRepository) : ViewModel() {

    private val _myPlan = MutableStateFlow<UserPlanResponse?>(null)
    val myPlan: StateFlow<UserPlanResponse?> = _myPlan

    private val _allPlans = MutableStateFlow<List<PlanInfo>>(emptyList())
    val allPlans: StateFlow<List<PlanInfo>> = _allPlans

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // --- Pass userId so backend reads real data from Supabase ---
    fun loadMyPlan(userId: String = "00000000-0000-0000-0000-000000000000") {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getMyPlan(userId)) {
                is Result.Success -> _myPlan.value = result.data
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun loadAllPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getAllPlans()) {
                is Result.Success -> _allPlans.value = result.data.plans
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun upgradePlan(plan: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.upgradePlan(plan)) {
                is Result.Success -> { /* reload handled by caller */ }
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }
}
