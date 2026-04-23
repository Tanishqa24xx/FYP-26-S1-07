package com.example.weblinkscanner.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.weblinkscanner.viewmodel.ScanViewModel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val BorderCol   = Color(0xFFCBD5E1)
private val GreenPass   = Color(0xFF16A34A)
private val GreenBg     = Color(0xFFDCFCE7)
private val RedFail     = Color(0xFFDC2626)

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
                .addOnFailureListener { e -> onError("OCR failed: ${e.message}"); proxy.close() }
        }
        override fun onError(e: ImageCaptureException) { onError("Capture error: ${e.message}") }
    })
}

@Composable
fun CameraScreen(
    viewModel:      ScanViewModel,
    userId:         String = "00000000-0000-0000-0000-000000000000",
    onScanComplete: () -> Unit,
    onBack:         () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context        = LocalContext.current

    var detectedUrl by remember { mutableStateOf("") }
    var statusText  by remember { mutableStateOf("") }
    var isCapturing by remember { mutableStateOf(false) }
    var urlDetected by remember { mutableStateOf(false) }

    val isLoading    by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scanResult   by viewModel.scanResult.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }

    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor  = remember { Executors.newSingleThreadExecutor() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageBgTop, PageBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Camera Scan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text("Point at a URL to detect and scan it", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(24.dp))

            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Blue100, RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory  = { ctx ->
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
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScreen", "Camera bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // URL input card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value         = detectedUrl,
                        onValueChange = { detectedUrl = it },
                        label         = { Text("Detected URL (editable)") },
                        leadingIcon   = { Icon(Icons.Default.Link, null, tint = Blue600) },
                        trailingIcon  = if (urlDetected) ({
                            Icon(Icons.Default.CheckCircle, null, tint = GreenPass)
                        }) else null,
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Blue600,
                            unfocusedBorderColor = BorderCol,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = Blue600
                        )
                    )

                    if (statusText.isNotBlank() || errorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = errorMessage ?: statusText,
                            color = if (errorMessage != null) RedFail else if (urlDetected) GreenPass else TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val ic = imageCaptureRef.value ?: return@OutlinedButton
                                isCapturing = true
                                detectedUrl = ""
                                urlDetected = false
                                statusText  = ""
                                viewModel.clearError()
                                captureAndOcr(
                                    imageCapture = ic,
                                    executor     = cameraExecutor,
                                    onResult     = { text, isUrl ->
                                        detectedUrl = text
                                        urlDetected = isUrl
                                        statusText  = if (isUrl) "URL detected!" else "No URL found — edit the field manually."
                                        isCapturing = false
                                    },
                                    onError = { msg -> statusText = msg; isCapturing = false }
                                )
                            },
                            enabled  = !isCapturing && !isLoading,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                        ) {
                            if (isCapturing) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Blue600, strokeWidth = 2.dp)
                            else {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Capture", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick  = {
                                if (detectedUrl.isNotBlank()) {
                                    viewModel.scanFromCamera(detectedUrl, userId)
                                    onScanComplete()
                                }
                            },
                            enabled  = detectedUrl.isNotBlank() && !isLoading,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Scan URL", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = Blue50),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, null, tint = Blue600, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Make sure the URL is clear and well-lit for best detection.",
                        fontSize = 12.sp, color = Blue600, lineHeight = 18.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back", fontWeight = FontWeight.SemiBold, color = Blue600)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
