package com.example.weblinkscanner.ui.screens.platform

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.PMSystemAlert
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenBg = Color(0xFFECFDF5)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMTeal     = Color(0xFF0891B2); private val PMTealBg  = Color(0xFFE0F2FE)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMSystemHealthScreen(
    token:     String,
    viewModel: PlatformViewModel,
    onBack:    () -> Unit
) {
    val healthState by viewModel.systemHealth.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSystemHealth(token) }

    Scaffold(
        containerColor = PMPageBg,
        topBar = {
            TopAppBar(
                title = { Text("System Health", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.loadSystemHealth(token) }) { Icon(Icons.Default.Refresh, null, tint = PMGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        when (val s = healthState) {
            is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
            is PlatformViewModel.UiState.Error   -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(s.message, color = PMRed) }
            is PlatformViewModel.UiState.Success -> {
                val health = s.data
                val (overallColor, overallBg, overallIcon) = when (health.overallStatus) {
                    "healthy"  -> Triple(PMGreen, PMGreenBg, Icons.Default.CheckCircle)
                    "degraded" -> Triple(PMAmber, PMAmberBg, Icons.Default.Warning)
                    else       -> Triple(PMRed,   PMRedBg,   Icons.Default.Error)
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Overall status banner
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = overallBg),
                        elevation= CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(overallIcon, null, tint = overallColor, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    when (health.overallStatus) {
                                        "healthy"  -> "All Systems Operational"
                                        "degraded" -> "System Degraded"
                                        else       -> "Critical Issues Detected"
                                    },
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = overallColor
                                )
                                Text("${health.alertCount} active alert${if (health.alertCount != 1) "s" else ""}",
                                    fontSize = 13.sp, color = overallColor.copy(alpha = 0.8f))
                            }
                        }
                    }

                    // Service status grid
                    Text("Service Status", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                    val serviceIcons = mapOf(
                        "database"     to Icons.Default.Storage,
                        "scan_service" to Icons.Default.QrCodeScanner,
                        "auth_service" to Icons.Default.Lock,
                        "api"          to Icons.Default.Api
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        health.services.entries.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (name, svc) ->
                                    val isOk = svc.status == "healthy"
                                    val icon = serviceIcons[name] ?: Icons.Default.Cloud
                                    Card(
                                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                                        colors   = CardDefaults.cardColors(containerColor = PMCardBg),
                                        elevation= CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()) {
                                                Icon(icon, null, tint = if (isOk) PMGreen else PMRed, modifier = Modifier.size(20.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = if (isOk) PMGreenBg else PMRedBg) {
                                                    Text(if (isOk) "OK" else "DOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                        color = if (isOk) PMGreen else PMRed,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(name.replace("_", " ").replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PMTxt)
                                            svc.responseMs?.let { ms ->
                                                Text("${ms}ms", fontSize = 11.sp, color = PMMuted)
                                            }
                                        }
                                    }
                                }
                                // Pad if odd number
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Active alerts
                    val alerts = health.activeAlerts
                    Text("Active Alerts (${alerts.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                    if (alerts.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PMGreenBg), elevation = CardDefaults.cardElevation(0.dp)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = PMGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("No active alerts — everything looks good!", fontSize = 13.sp, color = PMGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            alerts.forEach { alert -> AlertCard(alert = alert, onResolve = { viewModel.resolveAlert(token, alert.id) }) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun AlertCard(alert: PMSystemAlert, onResolve: () -> Unit) {
    val (alertColor, alertBg) = when (alert.severity) {
        "critical" -> PMRed   to PMRedBg
        "warning"  -> PMAmber to PMAmberBg
        else       -> PMTeal  to PMTealBg
    }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PMCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(8.dp), color = alertBg) {
                Icon(
                    when (alert.severity) { "critical" -> Icons.Default.Error; "warning" -> Icons.Default.Warning; else -> Icons.Default.Info },
                    null, tint = alertColor, modifier = Modifier.padding(6.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.type, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PMTxt)
                Text(alert.message, fontSize = 12.sp, color = PMMuted)
                Text(alert.createdAt?.take(16)?.replace("T", " ") ?: "", fontSize = 11.sp, color = PMMuted)
            }
            TextButton(onClick = onResolve, colors = ButtonDefaults.textButtonColors(contentColor = PMGreen)) {
                Text("Resolve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
