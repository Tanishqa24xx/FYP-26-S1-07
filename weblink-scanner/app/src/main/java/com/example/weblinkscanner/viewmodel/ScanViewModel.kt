// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\viewmodel\ScanViewModel.kt

package com.example.weblinkscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weblinkscanner.data.models.ScanResponse
import com.example.weblinkscanner.data.models.CameraScanResponse
import com.example.weblinkscanner.data.models.QRScanResponse
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanViewModel(private val repository: WeblinkScannerRepository) : ViewModel() {

    // Latest scan result (used by result screen)
    private val _scanResult = MutableStateFlow<ScanResponse?>(null)
    val scanResult: StateFlow<ScanResponse?> = _scanResult

    // Camera scan result
    private val _cameraResult = MutableStateFlow<CameraScanResponse?>(null)
    val cameraResult: StateFlow<CameraScanResponse?> = _cameraResult

    // QR scan result
    private val _qrResult = MutableStateFlow<QRScanResponse?>(null)
    val qrResult: StateFlow<QRScanResponse?> = _qrResult

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // -----------------------------------------
    // Manual URL scan
    // -----------------------------------------
    fun scanUrl(url: String) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            when (val result = repository.scanUrl(url)) {

                is Result.Success -> {
                    _scanResult.value = result.data
                }

                is Result.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    // -----------------------------------------
    // Camera scan
    // OCR text extracted by phone
    // -----------------------------------------
    fun scanFromCamera(text: String) {

        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            when (val result = repository.scanCamera(text)) {

                is Result.Success -> {
                    _cameraResult.value = result.data
                    _scanResult.value = result.data.scanResult
                }

                is Result.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    // -----------------------------------------
    // QR scan
    // -----------------------------------------
    fun scanQr(qrData: String) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            when (val result = repository.scanQr(qrData)) {

                is Result.Success -> {
                    _qrResult.value = result.data
                    _scanResult.value = result.data.scanResult
                }

                is Result.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    // Clear results when leaving result screen
    fun clearResult() {
        _scanResult.value = null
        _cameraResult.value = null
        _qrResult.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
