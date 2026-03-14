// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\CameraScreen.kt
// Camera OCR scan

package com.example.weblinkscanner.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.weblinkscanner.viewmodel.ScanViewModel
import java.util.concurrent.Executor
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Captures a photo, runs OCR, and returns the first detected URL (or raw text) via onResult.
// onResult(text, isUrl) — isUrl is true when a URL was found in the OCR output.
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun captureAndOcr(
    imageCapture: ImageCapture,
    executor: Executor,
    onResult: (text: String, isUrl: Boolean) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {

        override fun onCaptureSuccess(proxy: ImageProxy) {
            val mediaImage = proxy.image
            if (mediaImage == null) { proxy.close(); onError("No image captured"); return }

            val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { visionText ->
                    val urlMatch = Regex("https?://[^\\s]+").find(visionText.text)
                    onResult(urlMatch?.value ?: visionText.text, urlMatch != null)
                    proxy.close()
                }
                .addOnFailureListener { e ->
                    onError("OCR failed: ${e.message}")
                    proxy.close()
                }
        }

        override fun onError(e: ImageCaptureException) {
            onError("Capture error: ${e.message}")
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: ScanViewModel,
    onScanComplete: () -> Unit,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedUrl by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isCapturing by remember { mutableStateOf(false) }

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
    }*/

    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor  = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera capture") },
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

            Text("Point your camera at the link. We'll detect and extract the URL.")

            // Camera preview
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    ProcessCameraProvider.getInstance(ctx).addListener({
                        val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageCapture = ImageCapture.Builder().build()
                        imageCaptureRef.value = imageCapture
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            // Detected URL field — editable so user can correct OCR mistakes
            OutlinedTextField(
                value = detectedUrl,
                onValueChange = { detectedUrl = it },
                label = { Text("Detected URL (editable)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (statusText.isNotBlank() || errorMessage != null) {
                Text(
                    text = errorMessage ?: statusText,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Rescan — takes a new photo and runs OCR again
                OutlinedButton(
                    onClick = {
                        val ic = imageCaptureRef.value ?: return@OutlinedButton
                        isCapturing = true
                        detectedUrl = ""
                        viewModel.clearError()
                        captureAndOcr(
                            imageCapture = ic,
                            executor = cameraExecutor,
                            onResult = { text, isUrl ->
                                detectedUrl = text
                                statusText = if (isUrl) "URL detected." else "No URL found. You can edit the field."
                                isCapturing = false
                            },
                            onError = { msg ->
                                statusText = msg
                                isCapturing = false
                            }
                        )
                    },
                    enabled = !isCapturing && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCapturing) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text("Rescan")
                }

                // Scan URL — sends the (possibly edited) detected URL to the backend
                Button(
                    onClick = { if (detectedUrl.isNotBlank()) viewModel.scanFromCamera(detectedUrl) },
                    enabled = detectedUrl.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Scan URL")
                }
            }

            Text("Tip: Make sure the URL is clear and well-lit for best detection.", style = MaterialTheme.typography.bodySmall)

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
