package com.example.weblinkscanner.ui.screens

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.example.weblinkscanner.viewmodel.ScanViewModel
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

@Composable
fun QrScreen(
    viewModel:      ScanViewModel,
    userId:         String = "00000000-0000-0000-0000-000000000000",
    onScanComplete: () -> Unit,
    onBack:         () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var extractedUrl by remember { mutableStateOf("") }
    var isScanning   by remember { mutableStateOf(true) }
    var detected     by remember { mutableStateOf(false) }

    val isLoading    by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scanResult   by viewModel.scanResult.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

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
                Icon(Icons.Default.QrCodeScanner, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("QR Code Scanner", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text(
                if (detected) "QR code detected!" else "Align QR code inside the frame",
                fontSize = 14.sp,
                color    = if (detected) GreenPass else TextMuted
            )
            Spacer(Modifier.height(24.dp))

            // Camera preview
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, if (detected) GreenPass else Blue100, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory  = { ctx ->
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
                                        if (isScanning) {
                                            scanQrFrame(proxy) { rawValue ->
                                                extractedUrl = rawValue
                                                isScanning   = false
                                                detected     = true
                                            }
                                        } else proxy.close()
                                    }
                                }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyser)
                            } catch (e: Exception) { Log.e("QrScreen", "Camera bind failed", e) }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Extracted URL card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value         = extractedUrl,
                        onValueChange = { extractedUrl = it },
                        label         = { Text("Extracted URL") },
                        leadingIcon   = { Icon(Icons.Default.Link, null, tint = Blue600) },
                        trailingIcon  = if (detected) ({
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

                    errorMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = RedFail, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = {
                                extractedUrl = ""
                                isScanning   = true
                                detected     = false
                                viewModel.clearError()
                                viewModel.clearResult()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rescan", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick  = {
                                if (extractedUrl.isNotBlank()) {
                                    viewModel.scanQr(extractedUrl, userId)
                                    onScanComplete()
                                }
                            },
                            enabled  = extractedUrl.isNotBlank() && !isLoading,
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
                    Text("Only URLs are extracted. Non-link QR codes will show an error.",
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

@SuppressLint("UnsafeOptInUsageError")
private fun scanQrFrame(proxy: ImageProxy, onQrFound: (String) -> Unit) {
    val mediaImage = proxy.image
    if (mediaImage == null) { proxy.close(); return }
    val image   = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull { it.valueType == Barcode.TYPE_URL || it.rawValue != null }
                ?.rawValue?.let { onQrFound(it) }
        }
        .addOnCompleteListener { proxy.close() }
}
