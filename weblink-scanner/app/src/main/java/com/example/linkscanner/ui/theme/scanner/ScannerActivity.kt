package com.example.linkscanner.ui.scanner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkscanner.api.RetrofitClient
import com.example.linkscanner.model.ScanRequest
import com.example.linkscanner.model.ScanResponse
import com.example.linkscanner.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ── Colors — same palette as Login & Menu ──────────────────────────────────────
private val Blue600      = Color(0xFF2563EB)
private val Blue50       = Color(0xFFEFF6FF)
private val Blue100      = Color(0xFFDBEAFE)
private val PageBgTop    = Color(0xFFEFF6FF)
private val PageBgBot    = Color(0xFFF8FAFC)
private val CardBg       = Color.White
private val TextPrimary  = Color(0xFF0F172A)
private val TextMuted    = Color(0xFF64748B)
private val DividerCol   = Color(0xFFE2E8F0)
private val BorderCol    = Color(0xFFCBD5E1)

private val SafeGreen        = Color(0xFF16A34A)
private val SafeGreenBg      = Color(0xFFF0FDF4)
private val SafeGreenBorder  = Color(0xFFBBF7D0)
private val DangerRed        = Color(0xFFDC2626)
private val DangerRedBg      = Color(0xFFFEF2F2)
private val DangerRedBorder  = Color(0xFFFECACA)
private val WarnOrange       = Color(0xFFD97706)
private val WarnOrangeBg     = Color(0xFFFFFBEB)
private val WarnOrangeBorder = Color(0xFFFDE68A)

// Simple data class to hold recent scan entries
private data class RecentScan(val url: String, val riskLevel: String)

@Composable
fun ScannerScreen(
    userPlan: String = "FREE",
    scansRemaining: Int = 5,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var url            by remember { mutableStateOf("") }
    var isScanning     by remember { mutableStateOf(false) }
    var scanResult     by remember { mutableStateOf<ScanResponse?>(null) }
    var errorMsg       by remember { mutableStateOf("") }
    var recentScans    by remember { mutableStateOf<List<RecentScan>>(emptyList()) }

    val context            = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Pulsing animation while scanning
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.6f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    fun doScan() {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            errorMsg = "Please enter a URL to scan"
            return
        }
        keyboardController?.hide()
        isScanning = true
        errorMsg   = ""
        scanResult = null

        RetrofitClient.api.scanUrl(ScanRequest(trimmed))
            .enqueue(object : Callback<ScanResponse> {
                override fun onResponse(
                    call: Call<ScanResponse>,
                    response: Response<ScanResponse>
                ) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        isScanning = false
                        if (response.isSuccessful) {
                            val result = response.body()
                            scanResult = result
                            if (result != null) {
                                // Prepend to recent list, keep last 5
                                recentScans = (listOf(
                                    RecentScan(result.url, result.risk_level)
                                ) + recentScans).take(5)
                            }
                        } else {
                            errorMsg = "Scan failed (${response.code()}). Try again."
                        }
                    }
                }
                override fun onFailure(call: Call<ScanResponse>, t: Throwable) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        isScanning = false
                        errorMsg = "Network error: ${t.message ?: "Check your connection"}"
                    }
                }
            })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(PageBgTop, PageBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(56.dp))

            // ── Page Title ────────────────────────────────────────────────────
            Text(
                text = "Scan URL",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Plan Info Bar ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Blue50),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue100)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${userPlan.uppercase()} Plan  •  5 scans/day",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Blue600
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Blue100
                    ) {
                        Text(
                            text = "Remaining: $scansRemaining",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── URL Input Card ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            errorMsg = ""
                            scanResult = null
                        },
                        placeholder = {
                            Text("Paste URL here (https://...)", color = TextMuted)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Blue600)
                        },
                        singleLine = true,
                        isError = errorMsg.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction    = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { doScan() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Blue600,
                            unfocusedBorderColor = BorderCol,
                            errorBorderColor     = DangerRed,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = Blue600
                        )
                    )

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(errorMsg, color = DangerRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Scan Button ───────────────────────────────────────────
                    Button(
                        onClick  = { doScan() },
                        enabled  = !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = Blue600,
                            disabledContainerColor = Blue600.copy(alpha = if (isScanning) pulseAlpha else 0.6f)
                        )
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Scanning...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint     = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Camera / QR Row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Capture via Camera
                OutlinedButton(
                    onClick  = { /* TODO: camera intent */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                ) {
                    Icon(
                        imageVector    = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint     = Blue600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Camera",
                        fontSize    = 13.sp,
                        fontWeight  = FontWeight.SemiBold,
                        color       = Blue600
                    )
                }

                // Scan QR
                OutlinedButton(
                    onClick  = { /* TODO: QR scanner */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                ) {
                    Icon(
                        imageVector    = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint     = Blue600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Scan QR",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Blue600
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Scan Result ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = scanResult != null,
                enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit    = fadeOut()
            ) {
                scanResult?.let { result ->
                    val style = riskStyle(result.risk_level)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, style.borderColor, RoundedCornerShape(16.dp)),
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = style.bgColor),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = style.emoji, fontSize = 38.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text       = style.label,
                                fontSize   = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color      = style.textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text      = style.description,
                                fontSize  = 13.sp,
                                color     = style.textColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = style.borderColor)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text      = result.url,
                                fontSize  = 11.sp,
                                color     = style.textColor.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                                maxLines  = 2,
                                overflow  = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Recent Scans Card ─────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector    = Icons.Default.History,
                            contentDescription = null,
                            tint     = Blue600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text       = "Most Recent Scans",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (recentScans.isEmpty()) {
                        Text(
                            text      = "No scans yet — scan a link to see results here.",
                            fontSize  = 13.sp,
                            color     = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        recentScans.forEachIndexed { index, scan ->
                            if (index > 0) HorizontalDivider(
                                color     = DividerCol,
                                modifier  = Modifier.padding(vertical = 6.dp)
                            )
                            RecentScanRow(scan)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Divider ───────────────────────────────────────────────────────
            HorizontalDivider(color = DividerCol, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Back Button ───────────────────────────────────────────────────
            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Icon(
                    imageVector    = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint     = Blue600,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Back",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Blue600
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// ── Recent scan row ────────────────────────────────────────────────────────────
@Composable
private fun RecentScanRow(scan: RecentScan) {
    val style = riskStyle(scan.riskLevel)
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text     = scan.url,
            fontSize = 12.sp,
            color    = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = style.bgColor
        ) {
            Text(
                text       = style.label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = style.textColor,
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ── Risk style helper ──────────────────────────────────────────────────────────
private data class RiskStyle(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val emoji: String,
    val label: String,
    val description: String
)

private fun riskStyle(riskLevel: String) = when (riskLevel.lowercase()) {
    "safe"      -> RiskStyle(SafeGreenBg, SafeGreenBorder, SafeGreen,  "✅", "Safe",      "This link appears to be safe to visit.")
    "dangerous" -> RiskStyle(DangerRedBg, DangerRedBorder, DangerRed,  "🚨", "Dangerous", "Warning! This link may contain malware or phishing content.")
    else        -> RiskStyle(WarnOrangeBg, WarnOrangeBorder, WarnOrange, "⚠️", "Suspicious","This link looks suspicious. Proceed with caution.")
}