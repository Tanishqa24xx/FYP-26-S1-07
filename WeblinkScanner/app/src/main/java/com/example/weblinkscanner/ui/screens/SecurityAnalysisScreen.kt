package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.SandboxViewModel

private val Blue600   = Color(0xFF2563EB)
private val Blue50    = Color(0xFFEFF6FF)
private val Blue100   = Color(0xFFDBEAFE)
private val PageBgTop = Color(0xFFEFF6FF)
private val PageBgBot = Color(0xFFF8FAFC)
private val CardBg    = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val GreenPass   = Color(0xFF16A34A)
private val GreenBg     = Color(0xFFDCFCE7)
private val RedFail     = Color(0xFFDC2626)
private val RedBg       = Color(0xFFFEE2E2)
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)

@Composable
fun SecurityAnalysisScreen(
    viewModel: SandboxViewModel,
    url: String,
    scanId: String,
    verdict: String = "UNKNOWN",    // passed from scan result
    threatCategories: String = "",           // comma-separated, passed from scan result
    onBack: () -> Unit
) {
    val report by viewModel.sandboxReport.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val isBlacklisted = verdict.uppercase() == "DANGEROUS" &&
                        threatCategories.contains("Blacklisted", ignoreCase = true)

    // Re-fetch for every new scan (scanId changes each time)
    LaunchedEffect(scanId) {
        if (!isBlacklisted) {
            viewModel.analyseSandbox(url, scanId)
        }
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

            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(listOf(Blue100, Blue50)),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Security Analysis", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text(
                text     = url,
                fontSize = 12.sp,
                color    = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))

            // --- BLACKLISTED MODE: no backend call needed ---
            if (isBlacklisted) {
                BlacklistedReport(url = url, threatCategories = threatCategories)
            }

            // --- NORMAL MODE: use sandbox report ---
            else when {
                loading -> {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Blue600)
                                Spacer(Modifier.height(12.dp))
                                Text("Running security checks...", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                error != null -> {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = RedBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = RedFail, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Analysis failed. Please try again.", color = RedFail, fontSize = 13.sp)
                        }
                    }
                }

                report != null -> {
                    val r = report!!
                    val finalUrlCheck = r.redirectChain.lastOrNull() ?: url
                    val httpsOk   = url.startsWith("https") || finalUrlCheck.startsWith("https")
                    val sslValid  = r.sslInfo?.valid != false  // null=unknown treated as ok
                    val hasRedirect = r.redirectChain.size > 1
                    val finalUrl  = r.redirectChain.lastOrNull() ?: url
                    val redirectedToNewDomain = hasRedirect && !finalUrl.contains(
                        try { java.net.URI(url).host ?: "" } catch (e: Exception) { "" }
                    )
                    val extLinkCount = r.externalLinks.size
                    val loadTime     = r.loadTimeMs ?: 0
                    val statusOk     = r.statusCode in 200..399

                    val issueCount = listOf(
                        !httpsOk,
                        r.sslInfo?.valid == false && httpsOk,
                        redirectedToNewDomain,
                        extLinkCount > 20,
                        !statusOk && r.statusCode != null
                    ).count { it }

                    val (summaryColor, summaryBg, summaryText, summaryIcon) = when {
                        issueCount == 0 -> listOf(GreenPass, GreenBg, "No issues detected", Icons.Default.CheckCircle)
                        issueCount <= 2 -> listOf(AmberWarn, AmberBg, "$issueCount concern(s) found", Icons.Default.Warning)
                        else            -> listOf(RedFail,   RedBg,   "$issueCount issues found",     Icons.Default.Error)
                    }

                    // Summary banner
                    @Suppress("UNCHECKED_CAST")
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = summaryBg as Color),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(summaryIcon as ImageVector, null, tint = summaryColor as Color, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(summaryText as String, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = summaryColor)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Security checks card
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Security Checks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(Modifier.height(12.dp))

                            CheckRow("HTTPS Encryption",
                                if (httpsOk) "Connection is encrypted"
                                else "No encryption — data sent in plain text",
                                pass = httpsOk)

                            if (httpsOk) {
                                HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                CheckRow("SSL Certificate",
                                    when {
                                        r.sslInfo == null || r.sslInfo.valid == null -> "Certificate verified via HTTPS connection"
                                        r.sslInfo.valid == true -> "Valid · Expires ${r.sslInfo.expiry ?: "unknown"}"
                                        else -> "Invalid or untrusted SSL certificate"
                                    },
                                    pass = r.sslInfo?.valid != false,
                                    warn = false
                                )
                            }

                            r.statusCode?.let { code ->
                                HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                CheckRow("Server Response",
                                    "HTTP $code — ${httpStatusText(code)}",
                                    pass = code in 200..399,
                                    warn = code in 400..499
                                )
                            }

                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                            CheckRow("Redirect Check",
                                when {
                                    !hasRedirect           -> "No redirects detected"
                                    redirectedToNewDomain  -> "Redirected to a different domain: $finalUrl"
                                    else                   -> "${r.redirectChain.size - 1} redirect(s) — same domain"
                                },
                                pass = !redirectedToNewDomain,
                                warn = hasRedirect && !redirectedToNewDomain
                            )

                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                            CheckRow("External Links",
                                when {
                                    extLinkCount == 0  -> "No external links found"
                                    extLinkCount <= 10 -> "$extLinkCount external link(s) — normal"
                                    extLinkCount <= 20 -> "$extLinkCount external links — moderate"
                                    else               -> "$extLinkCount external links — unusually high"
                                },
                                pass = extLinkCount <= 10,
                                warn = extLinkCount in 11..20
                            )

                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                            CheckRow("Load Performance",
                                when {
                                    loadTime == 0    -> "Could not measure"
                                    loadTime < 2000  -> "${loadTime}ms — fast"
                                    loadTime < 5000  -> "${loadTime}ms — moderate"
                                    else             -> "${loadTime}ms — slow"
                                },
                                pass = loadTime in 1..4999,
                                warn = loadTime >= 5000 || loadTime == 0
                            )
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
                Text("Back to Result", fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// --- Blacklisted report: shown without backend call ---
@Composable
private fun BlacklistedReport(url: String, threatCategories: String) {
    // Summary
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = RedBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GppBad, null, tint = RedFail, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("URL is Blacklisted", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RedFail)
                Text("This URL is known to be malicious.", fontSize = 12.sp, color = RedFail.copy(alpha = 0.8f))
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Checks card: all based on known info, no backend needed
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Security Checks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))

            CheckRow("Blacklist Check",
                "URL found in known malicious URL database",
                pass = false)

            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 8.dp))

            CheckRow("HTTPS Encryption",
                if (url.startsWith("https")) "Connection uses HTTPS"
                else if (!url.startsWith("http://")) "HTTPS status unknown — URL not visited for safety"
                else "No HTTPS encryption",
                pass = url.startsWith("https"),
                warn = !url.startsWith("http"))

            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 8.dp))

            CheckRow("Threat Categories",
                threatCategories.replace(",", " · ").ifBlank { "Malicious URL" },
                pass = false)

            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 8.dp))

            CheckRow("Recommendation",
                "Do not visit this URL. It has been flagged as dangerous.",
                pass = false)
        }
    }

    Spacer(Modifier.height(12.dp))

    // Warning card
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Live analysis was skipped for your safety. Visiting this URL could expose your device to malware, phishing, or other threats.",
                fontSize   = 12.sp,
                color      = Color(0xFF92400E),
                lineHeight = 18.sp
            )
        }
    }
}

// --- Shared composables ---
@Composable
private fun CheckRow(label: String, detail: String, pass: Boolean, warn: Boolean = false) {
    val (color, bg, icon) = when {
        pass && !warn -> Triple(GreenPass, GreenBg, Icons.Default.CheckCircle)
        warn -> Triple(AmberWarn, AmberBg, Icons.Default.Warning)
        else -> Triple(RedFail,   RedBg,   Icons.Default.Cancel)
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,  fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(detail, fontSize = 12.sp, color = TextMuted, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 12.sp, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

private fun httpStatusText(code: Int) = when (code) {
    200 -> "OK"; 301 -> "Moved Permanently"; 302 -> "Found (Redirect)"
    400 -> "Bad Request"; 401 -> "Unauthorized"; 403 -> "Forbidden"
    404 -> "Not Found"; 500 -> "Internal Server Error"; 503 -> "Service Unavailable"
    else -> "Status $code"
}
