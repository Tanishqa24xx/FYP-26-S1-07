package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.PlanViewModel
import com.example.weblinkscanner.viewmodel.ScanViewModel
import com.example.weblinkscanner.utils.ScanLimitNotificationManager

private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val BorderCol   = Color(0xFFCBD5E1)
private val DividerCol  = Color(0xFFE2E8F0)
private val SafeGreen   = Color(0xFF16A34A)
private val SafeGreenBg = Color(0xFFDCFCE7)
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)
private val RedDanger   = Color(0xFFDC2626)
private val RedBg       = Color(0xFFFEE2E2)

@Composable
fun ScanUrlScreen(
    scanViewModel: ScanViewModel,
    planViewModel: PlanViewModel,
    userId: String = "00000000-0000-0000-0000-000000000000",
    onScanComplete: () -> Unit,
    onCameraClick: () -> Unit,
    onQrClick: () -> Unit,
    onBack: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }

    val isLoading by scanViewModel.isLoading.collectAsState()
    val errorMessage by scanViewModel.errorMessage.collectAsState()
    val myPlan by planViewModel.myPlan.collectAsState()
    val recentScans = scanViewModel.recentScans

    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // On entry: clear any previous scan result and reload plan quota
    LaunchedEffect(userId) {
        scanViewModel.clearResult()
        planViewModel.loadMyPlan(userId)
    }

    val planName   = myPlan?.currentPlan?.uppercase() ?: "FREE"
    val dailyLimit = myPlan?.dailyLimit ?: 5
    val scansToday = myPlan?.scansToday ?: 0
    val remaining  = (dailyLimit - scansToday).coerceAtLeast(0)

    val isFree       = planName.uppercase() == "FREE"
    val isStandard   = planName.uppercase() == "STANDARD"
    val hasLimit     = isFree || isStandard
    val limitReached = hasLimit && remaining == 0
    val notifEnabled = ScanLimitNotificationManager.isEnabled(context, userId)

    fun doScan() {
        if (urlInput.isBlank() || isLoading || limitReached) return
        keyboardController?.hide()
        scanViewModel.scanUrl(urlInput.trim(), userId)
        onScanComplete()
    }

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
                Icon(Icons.Default.Search, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("Scan URL", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text("Check if a link is safe", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(20.dp))

            // --- Plan quota bar (Free and Standard only) ---
            if (hasLimit && limitReached) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = RedBg),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border    = BorderStroke(1.dp, RedDanger.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, null, tint = RedDanger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Daily scan limit reached",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RedDanger)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "You have used all $dailyLimit scans for today. " +
                                    "Scans reset daily. Upgrade your plan for more scans.",
                            fontSize = 12.sp, color = RedDanger.copy(alpha = 0.85f), lineHeight = 18.sp
                        )
                    }
                }
            } else if (hasLimit && notifEnabled) {
                // Show remaining count; show amber warning when 1 or fewer left
                val isWarning = remaining <= 1
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = if (isWarning) Color(0xFFFEF3C7) else Blue50
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border    = BorderStroke(1.dp, if (isWarning) Color(0xFFD97706) else Blue100)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isWarning) Icons.Default.Warning else Icons.Default.Shield,
                                null,
                                tint = if (isWarning) Color(0xFFD97706) else Blue600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$planName Plan  •  $dailyLimit scans/day",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = if (isWarning) Color(0xFFD97706) else Blue600
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isWarning) Color(0xFFD97706).copy(alpha = 0.15f) else Blue100
                        ) {
                            Text(
                                "Remaining: $remaining",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (isWarning) Color(0xFFD97706) else Blue600,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            // If notifEnabled is false and limit not reached, show nothing (user opted out)

            Spacer(Modifier.height(14.dp))

            // --- URL input ---
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; scanViewModel.clearError() },
                        placeholder = { Text("Paste URL here (https://...)", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Link, null, tint = Blue600) },
                        singleLine  = true,
                        isError     = errorMessage != null,
                        modifier    = Modifier.fillMaxWidth(),
                        shape       = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction    = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { doScan() }),
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
                        Text(it, color = RedDanger, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick  = { doScan() },
                        enabled  = urlInput.isNotBlank() && !isLoading && !limitReached,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = if (limitReached) DividerCol else Blue600,
                            disabledContainerColor = DividerCol
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Scanning...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Scan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Camera / QR ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = { if (!limitReached) onCameraClick() },
                    enabled  = !limitReached,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor         = if (limitReached) TextMuted else Blue600,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null,
                        tint = if (limitReached) TextMuted else Blue600,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (limitReached) TextMuted else Blue600)
                }
                OutlinedButton(
                    onClick  = { if (!limitReached) onQrClick() },
                    enabled  = !limitReached,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor         = if (limitReached) TextMuted else Blue600,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, null,
                        tint = if (limitReached) TextMuted else Blue600,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan QR", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (limitReached) TextMuted else Blue600)
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Recent scans ---
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = Blue600, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Most Recent Scans", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (recentScans.isEmpty()) {
                        Text("No scans yet - scan a link to see results here.",
                            fontSize = 13.sp, color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        recentScans.forEachIndexed { index, (url, risk) ->
                            if (index > 0) HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 6.dp))
                            val (badgeColor, badgeBg) = when (risk.uppercase()) {
                                "SAFE"       -> Pair(SafeGreen, SafeGreenBg)
                                "SUSPICIOUS" -> Pair(AmberWarn, AmberBg)
                                "DANGEROUS"  -> Pair(RedDanger, RedBg)
                                else         -> Pair(TextMuted,  Blue50)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = url, fontSize = 12.sp, color = TextMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                                    Text(text = risk.uppercase(), fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold, color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}