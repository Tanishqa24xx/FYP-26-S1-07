package com.example.weblinkscanner.ui.screens.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenLt = Color(0xFFD1FAE5); private val PMGreenBg = Color(0xFFECFDF5)
private val PMTeal     = Color(0xFF0891B2); private val PMTealBg  = Color(0xFFE0F2FE)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMPurple   = Color(0xFF7C3AED); private val PMPurpleBg= Color(0xFFF3E8FF)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMDashboardScreen(
    pmName:    String,
    pmEmail:   String,
    token:     String,
    viewModel: PlatformViewModel,
    onNavigateToPlans:     () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReports:   () -> Unit,
    onNavigateToSupport:   () -> Unit,
    onNavigateToFaq:       () -> Unit,
    onNavigateToHealth:    () -> Unit,
    onNavigateToSettings:  () -> Unit,
    onLogout:              () -> Unit
) {
    val overviewState by viewModel.analyticsOverview.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadAnalyticsOverview(token) }

    Scaffold(
        containerColor = PMPageBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Platform Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Platform Manager", fontSize = 12.sp, color = PMMuted)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, null, tint = PMGreen)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, null, tint = PMRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── PM Identity Card ─────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = PMGreen),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initials = pmName.trim().split(" ").filter { it.isNotBlank() }
                        .take(2).joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "PM" }
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(pmName.ifBlank { "Platform Manager" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(pmEmail, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                            Text("Platform Manager", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // ── Stats Grid ───────────────────────────────────────────────────
            val overview = (overviewState as? PlatformViewModel.UiState.Success)?.data
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PMStatCard("Total Users", overview?.totalUsers?.toString() ?: "—", Icons.Default.People, PMTeal, PMTealBg, Modifier.weight(1f))
                PMStatCard("Scans Today", overview?.scansToday?.toString() ?: "—", Icons.Default.QrCodeScanner, PMGreen, PMGreenBg, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PMStatCard("Monthly Scans", overview?.scansThisMonth?.toString() ?: "—", Icons.Default.BarChart, PMPurple, PMPurpleBg, Modifier.weight(1f))
                PMStatCard("Active Users", overview?.activeUsers?.toString() ?: "—", Icons.Default.HowToReg, PMAmber, PMAmberBg, Modifier.weight(1f))
            }

            // ── Navigation Grid ──────────────────────────────────────────────
            Text("Management", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PMNavCard("Subscription Plans", Icons.Default.CreditCard,   PMGreen,  PMGreenBg,  Modifier.weight(1f), onNavigateToPlans)
                    PMNavCard("Analytics",          Icons.Default.BarChart,     PMTeal,   PMTealBg,   Modifier.weight(1f), onNavigateToAnalytics)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PMNavCard("Reports",            Icons.Default.Description,  PMPurple, PMPurpleBg, Modifier.weight(1f), onNavigateToReports)
                    PMNavCard("Support Requests",   Icons.Default.HeadsetMic,   PMAmber,  PMAmberBg,  Modifier.weight(1f), onNavigateToSupport)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PMNavCard("FAQ Management",     Icons.Default.LiveHelp,     PMTeal,   PMTealBg,   Modifier.weight(1f), onNavigateToFaq)
                    PMNavCard("System Health",      Icons.Default.MonitorHeart, PMRed,    PMRedBg,    Modifier.weight(1f), onNavigateToHealth)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PMStatCard(label: String, value: String, icon: ImageVector, color: Color, bg: Color, modifier: Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PMCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = PMMuted)
        }
    }
}

@Composable
private fun PMNavCard(label: String, icon: ImageVector, color: Color, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PMCardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PMTxt, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
