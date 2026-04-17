package com.example.weblinkscanner.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weblinkscanner.data.models.NewScanResponse
import com.example.weblinkscanner.data.models.CameraScanResponse
import com.example.weblinkscanner.data.models.QRScanResponse
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanViewModel(private val repository: WeblinkScannerRepository) : ViewModel() {

    private val _scanResult = MutableStateFlow<NewScanResponse?>(null)
    val scanResult: StateFlow<NewScanResponse?> = _scanResult

    private val _cameraResult = MutableStateFlow<CameraScanResponse?>(null)
    val cameraResult: StateFlow<CameraScanResponse?> = _cameraResult

    private val _qrResult = MutableStateFlow<QRScanResponse?>(null)
    val qrResult: StateFlow<QRScanResponse?> = _qrResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // --- Persists across navigation. holds last 5 scans this session ---
    val recentScans = mutableStateListOf<Pair<String, String>>()  // url to riskLevel

    private fun addToRecent(url: String, riskLevel: String) {
        recentScans.removeAll { it.first == url }
        recentScans.add(0, Pair(url, riskLevel))
        if (recentScans.size > 5) recentScans.removeLast()
    }

    fun scanUrl(url: String, userId: String = "00000000-0000-0000-0000-000000000000") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.scanUrl(url, userId)) {
                is Result.Success -> {
                    _scanResult.value = result.data
                    addToRecent(result.data.url ?: url, result.data.riskLevel)
                }
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun scanFromCamera(text: String, userId: String = "00000000-0000-0000-0000-000000000000") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.scanCamera(text, userId)) {
                is Result.Success -> {
                    _cameraResult.value = result.data
                    _scanResult.value = result.data.scanResult
                    result.data.scanResult?.let {
                        addToRecent(it.url ?: "", it.riskLevel)
                    }
                }
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun scanQr(qrData: String, userId: String = "00000000-0000-0000-0000-000000000000") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.scanQr(qrData, userId)) {
                is Result.Success -> {
                    _qrResult.value = result.data
                    _scanResult.value = result.data.scanResult
                    result.data.scanResult?.let {
                        addToRecent(it.url ?: "", it.riskLevel)
                    }
                }
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearResult() {
        _scanResult.value = null
        _cameraResult.value = null
        _qrResult.value = null
    }

    fun clearError() { _errorMessage.value = null }

    fun clearRecentScans() { recentScans.clear() }
}
