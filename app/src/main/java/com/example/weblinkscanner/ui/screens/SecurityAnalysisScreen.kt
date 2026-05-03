package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.SandboxViewModel
import androidx.compose.ui.text.font.FontWeight

private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
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
    userId: String,
    verdict: String = "UNKNOWN",
    threatCategories: String = "",
    userPlan: String = "free",
    onBack: () -> Unit
) {
    val isStandard = userPlan.lowercase() in listOf("standard", "premium")
    val isPremium  = userPlan.lowercase() == "premium"

    val report  by viewModel.sandboxReport.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error   by viewModel.error.collectAsState()

    val isBlacklisted = verdict.uppercase() == "DANGEROUS" &&
            threatCategories.contains("Blacklisted", ignoreCase = true)

    LaunchedEffect(scanId) {
        if (!isBlacklisted) {
            viewModel.analyseSandbox(url, scanId, userId)
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

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
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

            if (isBlacklisted) {
                BlacklistedReport(url = url, threatCategories = threatCategories)
            } else {
                when {
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
                        val finalUrlCheck         = r.redirectChain.lastOrNull() ?: url
                        val httpsOk               = url.startsWith("https") || finalUrlCheck.startsWith("https")
                        val hasRedirect           = r.redirectChain.size > 1
                        val finalUrl              = r.redirectChain.lastOrNull() ?: url
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

                        val summaryColor: Color
                        val summaryBg: Color
                        val summaryText: String
                        val summaryIcon: ImageVector
                        when {
                            issueCount == 0 -> { summaryColor = GreenPass; summaryBg = GreenBg; summaryText = "No issues detected";          summaryIcon = Icons.Default.CheckCircle }
                            issueCount <= 2 -> { summaryColor = AmberWarn; summaryBg = AmberBg; summaryText = "$issueCount concern(s) found"; summaryIcon = Icons.Default.Warning }
                            else            -> { summaryColor = RedFail;   summaryBg = RedBg;   summaryText = "$issueCount issues found";     summaryIcon = Icons.Default.Error }
                        }

                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = summaryBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(summaryIcon, null, tint = summaryColor, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(summaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = summaryColor)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        val planLabel = when {
                            isPremium  -> "Advanced Security Analysis"
                            isStandard -> "Detailed Security Analysis"
                            else       -> "Standard Security Analysis"
                        }

                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(planLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            isPremium  -> Color(0xFFFEF3C7)
                                            isStandard -> Blue50
                                            else       -> Color(0xFFF1F5F9)
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                isPremium  -> "PREMIUM"
                                                isStandard -> "STANDARD"
                                                else       -> "FREE"
                                            },
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isPremium  -> Color(0xFFD97706)
                                                isStandard -> Blue600
                                                else       -> Color(0xFF64748B)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                CheckRow(
                                    label  = "HTTPS Encryption",
                                    detail = if (httpsOk) "Connection is encrypted" else "No encryption - data sent in plain text",
                                    pass   = httpsOk
                                )

                                if (httpsOk) {
                                    HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                    CheckRow(
                                        label  = "SSL Certificate",
                                        detail = when {
                                            r.sslInfo == null || r.sslInfo.valid == null -> "Certificate present (HTTPS active)"
                                            r.sslInfo.valid == true ->
                                                if (isStandard) "Valid · Expires ${r.sslInfo.expiry ?: "unknown"}"
                                                else "Valid"
                                            else -> "Invalid or untrusted SSL certificate"
                                        },
                                        pass = r.sslInfo?.valid != false,
                                        warn = false
                                    )
                                }

                                r.statusCode?.let { code ->
                                    HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                    CheckRow(
                                        label  = "Server Response",
                                        detail = "HTTP $code - ${httpStatusText(code)}",
                                        pass   = code in 200..399,
                                        warn   = code in 400..499
                                    )
                                }

                                HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                // Redirect Check with inline URL list for Standard & Premium
                                val redirectSummary = when {
                                    !hasRedirect          -> "No redirects detected"
                                    redirectedToNewDomain -> "Redirected to a different domain"
                                    else                  -> "${r.redirectChain.size - 1} redirect(s) - same domain"
                                }
                                if ((isStandard || isPremium) && hasRedirect && r.redirectChain.isNotEmpty()) {
                                    // Custom row: CheckRow icon + label, then URLs below
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                        val redirectPass = !redirectedToNewDomain
                                        val redirectWarn = hasRedirect && !redirectedToNewDomain
                                        val iconColor = when {
                                            redirectPass && !redirectWarn -> GreenPass
                                            redirectWarn                  -> AmberWarn
                                            else                          -> RedFail
                                        }
                                        val iconBg = when {
                                            redirectPass && !redirectWarn -> GreenBg
                                            redirectWarn                  -> AmberBg
                                            else                          -> RedBg
                                        }
                                        val icon = when {
                                            redirectPass && !redirectWarn -> Icons.Default.CheckCircle
                                            redirectWarn                  -> Icons.Default.Warning
                                            else                          -> Icons.Default.Cancel
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(iconBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Redirect Check", fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text(redirectSummary, fontSize = 12.sp,
                                                color = TextMuted, lineHeight = 18.sp)
                                            Spacer(Modifier.height(4.dp))
                                            val displayed = r.redirectChain.take(2)
                                            displayed.forEachIndexed { i, step ->
                                                Text(
                                                    "${i + 1}. $step",
                                                    fontSize = 11.sp,
                                                    color    = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (r.redirectChain.size > 2) {
                                                Text(
                                                    "+${r.redirectChain.size - 2} more",
                                                    fontSize = 11.sp,
                                                    color    = Blue600,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    CheckRow(
                                        label  = "Redirect Check",
                                        detail = redirectSummary,
                                        pass   = !redirectedToNewDomain,
                                        warn   = hasRedirect && !redirectedToNewDomain
                                    )
                                }

                                HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                CheckRow(
                                    label  = "External Links",
                                    detail = when {
                                        extLinkCount == 0  -> "No external links found"
                                        extLinkCount <= 10 -> "$extLinkCount external link(s) - normal"
                                        extLinkCount <= 20 -> "$extLinkCount external links - moderate"
                                        else               -> "$extLinkCount external links - unusually high"
                                    },
                                    pass = extLinkCount <= 10,
                                    warn = extLinkCount in 11..20
                                )

                                HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                CheckRow(
                                    label  = "Load Performance",
                                    detail = when {
                                        loadTime == 0   -> "Could not measure"
                                        loadTime < 2000 -> "${loadTime}ms - fast"
                                        loadTime < 5000 -> "${loadTime}ms - moderate"
                                        else            -> "${loadTime}ms - slow"
                                    },
                                    pass = loadTime in 1..4999,
                                    warn = loadTime >= 5000 || loadTime == 0
                                )

                                if (isPremium) {
                                    r.domainCount?.let {
                                        HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                        CheckRow("Domains Contacted", "$it domain(s) during page load", pass = it <= 10, warn = it in 11..20)
                                    }
                                    r.ipCount?.let {
                                        HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                        CheckRow("IPs Contacted", "$it unique IP address(es)", pass = it <= 5, warn = it in 6..15)
                                    }
                                    if (r.consoleMessages.isNotEmpty()) {
                                        HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                        CheckRow(
                                            label  = "Browser Console",
                                            detail = "${r.consoleMessages.size} error/warning(s) recorded during load",
                                            pass   = false,
                                            warn   = r.consoleMessages.size in 1..3
                                        )
                                    }
                                    if (r.techDetected.isNotEmpty()) {
                                        HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                        CheckRow(
                                            label  = "Technologies",
                                            detail = r.techDetected.take(5).joinToString(", "),
                                            pass   = true,
                                            warn   = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Premium ad/tracker section
                if (isPremium && report != null) {
                    val r = report!!
                    Spacer(Modifier.height(12.dp))

                    if (r.adHeavy) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = AmberBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = AmberWarn, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Ad-Heavy Website Detected",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = AmberWarn
                                    )
                                    Text(
                                        "This page loads ${r.detectedAdTech.size + r.detectedTrackers.size} ad network(s) or tracker(s). " +
                                        "Ad-heavy pages may expose you to malvertising or aggressive data collection.",
                                        fontSize   = 12.sp,
                                        color      = Color(0xFF92400E),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrackChanges, null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Behaviour & Privacy Analysis", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(Modifier.height(12.dp))

                            CheckRow(
                                label  = "Ad Networks",
                                detail = if (r.detectedAdTech.isEmpty()) "No ad networks detected"
                                         else r.detectedAdTech.take(5).joinToString(", "),
                                pass   = r.detectedAdTech.isEmpty(),
                                warn   = r.detectedAdTech.isNotEmpty()
                            )

                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))

                            CheckRow(
                                label  = "Trackers",
                                detail = when {
                                    r.detectedTrackers.isEmpty() -> "No known trackers detected"
                                    r.detectedTrackers.size == 1 -> r.detectedTrackers[0]
                                    else -> "${r.detectedTrackers.size} trackers detected: ${r.detectedTrackers.take(3).joinToString(", ")}"
                                },
                                pass = r.detectedTrackers.isEmpty(),
                                warn = r.detectedTrackers.size in 1..3
                            )

                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))

                            CheckRow(
                                label  = "External Scripts",
                                detail = when {
                                    r.suspiciousScripts.isEmpty() -> "No suspicious external scripts detected"
                                    r.suspiciousScripts.size == 1 -> "1 unrecognised external script loaded"
                                    else -> "${r.suspiciousScripts.size} unrecognised external scripts loaded"
                                },
                                pass = r.suspiciousScripts.isEmpty(),
                                warn = r.suspiciousScripts.size in 1..4
                            )

                            if (r.suspiciousScripts.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                r.suspiciousScripts.take(5).forEach { script ->
                                    Text(
                                        "• $script",
                                        fontSize = 10.sp,
                                        color    = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 40.dp, bottom = 2.dp)
                                    )
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
                Text("Back to Result", fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BlacklistedReport(url: String, threatCategories: String) {
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

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Security Checks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))

            CheckRow("Blacklist Check", "URL found in known malicious URL database", pass = false)

            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))

            CheckRow(
                label  = "HTTPS Encryption",
                detail = if (url.startsWith("https")) "Connection uses HTTPS"
                         else if (!url.startsWith("http://")) "HTTPS status unknown - URL not visited for safety"
                         else "No HTTPS encryption",
                pass   = url.startsWith("https"),
                warn   = !url.startsWith("http")
            )

            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))

            CheckRow(
                label  = "Threat Categories",
                detail = threatCategories.replace(",", " · ").ifBlank { "Malicious URL" },
                pass   = false
            )

            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))

            CheckRow(
                label  = "Recommendation",
                detail = "Do not visit this URL. It has been flagged as dangerous.",
                pass   = false
            )
        }
    }

    Spacer(Modifier.height(12.dp))

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

@Composable
private fun CheckRow(label: String, detail: String, pass: Boolean, warn: Boolean = false) {
    val color: Color
    val bg: Color
    val icon: ImageVector
    when {
        pass && !warn -> { color = GreenPass; bg = GreenBg; icon = Icons.Default.CheckCircle }
        warn          -> { color = AmberWarn; bg = AmberBg; icon = Icons.Default.Warning }
        else          -> { color = RedFail;   bg = RedBg;   icon = Icons.Default.Cancel }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg),
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

private fun httpStatusText(code: Int) = when (code) {
    200  -> "OK"
    301  -> "Moved Permanently"
    302  -> "Found (Redirect)"
    400  -> "Bad Request"
    401  -> "Unauthorized"
    403  -> "Forbidden"
    404  -> "Not Found"
    500  -> "Internal Server Error"
    503  -> "Service Unavailable"
    else -> "Status $code"
}
