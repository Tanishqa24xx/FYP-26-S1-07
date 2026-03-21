package com.example.weblinkscanner.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import com.example.weblinkscanner.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

// --- Colors ---
private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val SafeGreen      = Color(0xFF16A34A)
private val SafeGreenBg    = Color(0xFFF0FDF4)
private val SafeGreenBorder = Color(0xFFBBF7D0)
private val WarnOrange     = Color(0xFFD97706)
private val WarnOrangeBg   = Color(0xFFFFFBEB)
private val WarnBorder     = Color(0xFFFDE68A)
private val DangerRed      = Color(0xFFDC2626)
private val DangerRedBg    = Color(0xFFFEF2F2)
private val DangerBorder   = Color(0xFFFECACA)
private val AmberBanner    = Color(0xFFFEF3C7)
private val AmberText      = Color(0xFF92400E)

private data class RiskStyle(
    val bg: Color, val border: Color, val text: Color,
    val emoji: String, val label: String
)

private fun riskStyle(level: String) = when (level.lowercase()) {
    "safe" -> RiskStyle(SafeGreenBg, SafeGreenBorder, SafeGreen,  "✅", "SAFE")
    "dangerous" -> RiskStyle(DangerRedBg, DangerBorder,    DangerRed,  "🚨", "DANGEROUS")
    else -> RiskStyle(WarnOrangeBg, WarnBorder,     WarnOrange, "⚠️", "SUSPICIOUS")
}

@Composable
fun ScanResultScreen(
    viewModel: ScanViewModel,
    repository: WeblinkScannerRepository,
    userId: String = "00000000-0000-0000-0000-000000000000",
    onSandboxClick: (url: String, scanId: String) -> Unit,
    onSecurityAnalysisClick:(url: String, scanId: String, verdict: String, categories: String) -> Unit,
    onBack: () -> Unit
) {
    val result by viewModel.scanResult.collectAsState()
    val scope = rememberCoroutineScope()

    var bannerDismissed by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }  // "saved" | "already" | "error"
    var isSaving by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageBgTop, PageBgBot)))
    ) {
        if (result == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Blue600)
                Spacer(Modifier.height(16.dp))
                Text("Scanning...", color = TextMuted, fontSize = 14.sp)
            }
            return@Box
        }

        val scan  = result!!
        val style = riskStyle(scan.riskLevel)
        val remaining = scan.scansRemaining

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(52.dp))

            // --- Header ---
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(listOf(Blue100, Blue50)),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(style.emoji, fontSize = 30.sp)
            }

            Spacer(Modifier.height(10.dp))

            Text("Scan Result", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)

            Spacer(Modifier.height(20.dp))

            // --- Scans remaining banner ---
            AnimatedVisibility(
                visible = remaining != null && !bannerDismissed,
                enter   = slideInVertically() + fadeIn(),
                exit    = slideOutVertically() + fadeOut()
            ) {
                if (remaining != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = AmberBanner),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AmberText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (remaining == 0)
                                        "You've used all your daily scans!"
                                    else
                                        "You have $remaining daily scan${if (remaining == 1) "" else "s"} left!",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = AmberText
                                )
                            }
                            IconButton(
                                onClick  = { bannerDismissed = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = AmberText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // --- Verdict card ---
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, style.border, RoundedCornerShape(16.dp)),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = style.bg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(style.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text       = "Verdict: ${style.label}",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = style.text
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- Scanned URL card ---
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = "Scanned URL",
                        fontSize   = 12.sp,
                        color      = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = scan.url ?: "N/A",
                        fontSize = 13.sp,
                        color    = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- Why this result card ---
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = "Why this result:",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )
                    HorizontalDivider(color = DividerCol)
                    if (scan.threatCategories.isNotEmpty()) {
                        scan.threatCategories.forEach { reason ->
                            Text("• $reason", fontSize = 13.sp, color = TextMuted)
                        }
                    } else {
                        Text(
                            "• ${scan.message ?: "No issues detected"}",
                            fontSize = 13.sp,
                            color    = TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- Action buttons ---

            Button(
                onClick  = { onSecurityAnalysisClick(
                    scan.url ?: "",
                    scan.scanId,
                    scan.riskLevel,
                    scan.threatCategories.joinToString(",")
                ) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Security Analysis", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick  = { onSandboxClick(scan.url ?: "", scan.scanId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Link in Sandbox", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            // --- Save Link button ---
            val saveColor = when (saveStatus) {
                "saved"   -> SafeGreen
                "already" -> WarnOrange
                "error"   -> DangerRed
                else      -> Blue600
            }
            val saveLabel = when (saveStatus) {
                "saved"   -> "Link Saved!"
                "already" -> "Already Saved"
                "error"   -> "Save Failed — Retry"
                else      -> "Save Link"
            }

            OutlinedButton(
                onClick  = {
                    if (isSaving || saveStatus == "saved") return@OutlinedButton
                    isSaving = true
                    scope.launch {
                        val r = repository.saveLink(
                            userId    = userId,
                            url       = scan.url ?: "",
                            scanId    = scan.scanId,
                            riskLevel = scan.riskLevel
                        )
                        when (r) {
                            is com.example.weblinkscanner.data.repository.Result.Success -> {
                                saveStatus = if (r.data["already_saved"] == "true") "already" else "saved"
                            }
                            is com.example.weblinkscanner.data.repository.Result.Error -> {
                                saveStatus = "error"
                            }
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = saveColor)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Blue600
                    )
                } else {
                    Icon(Icons.Default.BookmarkAdd, null, tint = saveColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(saveLabel, fontWeight = FontWeight.SemiBold, color = saveColor)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick  = { viewModel.clearResult(); onBack() },
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
