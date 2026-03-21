package com.example.weblinkscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weblinkscanner.data.models.SandboxReport
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SandboxViewModel(private val repository: WeblinkScannerRepository) : ViewModel() {

    private val _sandboxReport = MutableStateFlow<SandboxReport?>(null)
    val sandboxReport: StateFlow<SandboxReport?> = _sandboxReport

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun analyseSandbox(url: String, scanId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _sandboxReport.value = null
            when (val result = repository.analyseSandbox(url, scanId)) {
                is Result.Success -> _sandboxReport.value = result.data
                is Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearReport() {
        _sandboxReport.value = null
        _error.value = null
    }
}
