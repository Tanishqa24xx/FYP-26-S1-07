// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\QrScreen.kt
// QR Code scan

// 1. CameraX preview runs continuously
// 2. ML Kit checks every frame for a QR code automatically
// 3. First QR detected is sent to the backend via ScanViewModel
// 4. Navigates to ScanResultScreen when done

package com.example.weblinkscanner.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.example.weblinkscanner.viewmodel.ScanViewModel
import java.util.concurrent.Executors
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(
    viewModel: ScanViewModel,
    onScanComplete: () -> Unit,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var extractedUrl by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(true) }   // true = camera is actively looking

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    LaunchedEffect(scanResult) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        if (scanResult != null) onScanComplete()
    }
    /*
    LaunchedEffect(Unit) {
    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
}     */

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {

            // Instruction text
            Text("Align the QR code inside the frame to extract the URL.")

            // QR camera frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val future = ProcessCameraProvider.getInstance(ctx)

                        future.addListener({
                            val cameraProvider = future.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analyser = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor) { proxy ->
                                        // Only scan frames when actively looking
                                        if (isScanning) {
                                            scanQrFrame(proxy) { rawValue ->
                                                extractedUrl = rawValue
                                                isScanning = false   // stop after first detection
                                            }
                                        } else {
                                            proxy.close()
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analyser
                                )
                            } catch (e: Exception) {
                                Log.e("QrScreen", "Camera bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator()
                }
            }

            // Extracted URL field (read-only display)
            OutlinedTextField(
                value = extractedUrl,
                onValueChange = { extractedUrl = it },
                label = { Text("Extracted URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Error message
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Scan again and Scan URL buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Scan again — clears extracted URL and resumes camera scanning
                OutlinedButton(
                    onClick = {
                        extractedUrl = ""
                        isScanning = true
                        viewModel.clearError()
                        viewModel.clearResult()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan again")
                }

                // Scan URL — sends extracted URL to backend
                Button(
                    onClick = {
                        if (extractedUrl.isNotBlank()) {
                            viewModel.scanQr(extractedUrl)
                        }
                    },
                    enabled = extractedUrl.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Scan URL")
                    }
                }
            }

            // Tip text
            Text(
                "Tip: Only URLs are extracted. Non-link QR codes will show an error.",
                style = MaterialTheme.typography.bodySmall
            )

            // Back button
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

// Reads one camera frame and calls onQrFound if a QR code is detected
@SuppressLint("UnsafeOptInUsageError")
private fun scanQrFrame(proxy: ImageProxy, onQrFound: (String) -> Unit) {
    val mediaImage = proxy.image
    if (mediaImage == null) { proxy.close(); return }

    val image   = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()

    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes
                .firstOrNull { it.valueType == Barcode.TYPE_URL || it.rawValue != null }
                ?.rawValue
                ?.let { onQrFound(it) }
        }
        .addOnCompleteListener { proxy.close() }
}