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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.SandboxViewModel

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
fun SandboxScreen(
    viewModel: SandboxViewModel,
    url:       String,
    scanId:    String,
    onBack:    () -> Unit
) {
    val report  by viewModel.sandboxReport.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error   by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.analyseSandbox(url, scanId) }

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
                Icon(Icons.Default.BugReport, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Sandbox Analysis", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text(url, fontSize = 12.sp, color = TextMuted, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(24.dp))

            when {
                loading -> Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Blue600)
                            Spacer(Modifier.height(12.dp))
                            Text("Running sandbox analysis...", color = TextMuted, fontSize = 13.sp)
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
                            Text(error!!, color = RedFail, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick  = { viewModel.analyseSandbox(url, scanId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
                    ) { Text("Retry", fontWeight = FontWeight.SemiBold) }
                }

                report != null -> {
                    val r = report!!

                    // --- Overview card ---
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Overview", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(Modifier.height(12.dp))
                            SandboxRow(Icons.Default.Title,       "Page Title",  r.pageTitle  ?: "Could not retrieve")
                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                            SandboxRow(Icons.Default.Router,      "IP Address",  r.ipAddress  ?: "Unknown")
                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                            SandboxRow(Icons.Default.Speed,       "Load Time",   if ((r.loadTimeMs ?: 0) > 0) "${r.loadTimeMs}ms" else "Could not measure")
                            r.statusCode?.let {
                                HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                                SandboxRow(Icons.Default.CheckCircle, "HTTP Status", "HTTP $it")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- SSL card ---
                    r.sslInfo?.let { ssl ->
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = if (ssl.valid == true) GreenBg else RedBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (ssl.valid == true) Icons.Default.Lock else Icons.Default.LockOpen,
                                        null,
                                        tint     = if (ssl.valid == true) GreenPass else RedFail,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (ssl.valid == true) "SSL Certificate Valid" else "SSL Certificate Invalid",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = if (ssl.valid == true) GreenPass else RedFail
                                    )
                                }
                                ssl.expiry?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Expires: $it", fontSize = 12.sp, color = TextMuted)
                                }
                                ssl.issuer?.let {
                                    Text("Issuer: ${it.take(60)}", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // --- Redirects card ---
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SwapHoriz, null, tint = Blue600, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Redirect Chain", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(8.dp), color = if (r.redirectChain.size > 1) AmberBg else GreenBg) {
                                    Text(
                                        "${r.redirectChain.size} hop(s)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (r.redirectChain.size > 1) AmberWarn else GreenPass,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            if (r.redirectChain.isEmpty()) {
                                Text("No redirects detected", fontSize = 13.sp, color = TextMuted)
                            } else {
                                r.redirectChain.forEachIndexed { index, step ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("${index + 1}.", fontSize = 12.sp, color = TextMuted, modifier = Modifier.width(20.dp))
                                        Text(step, fontSize = 12.sp, color = TextPrimary, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- External links card ---
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, null, tint = Blue600, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("External Links", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = if (r.externalLinks.size > 20) AmberBg else GreenBg) {
                                    Text(
                                        "${r.externalLinks.size} found",
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = if (r.externalLinks.size > 20) AmberWarn else GreenPass,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (r.externalLinks.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                r.externalLinks.take(5).forEach { link ->
                                    Text("• $link", fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(2.dp))
                                }
                                if (r.externalLinks.size > 5) {
                                    Text("...and ${r.externalLinks.size - 5} more", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    // --- Note ---
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = Blue50),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = Blue600, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sandbox analysis may take longer on slow connections.",
                                fontSize = 12.sp, color = Blue600, lineHeight = 18.sp)
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
private fun SandboxRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Blue600, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
