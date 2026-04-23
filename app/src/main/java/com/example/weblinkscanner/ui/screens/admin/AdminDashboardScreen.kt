package com.example.weblinkscanner.ui.screens.admin

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.AdminStatsResponse
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val AdminBlue    = Color(0xFF1D4ED8)
private val AdminBlueBg  = Color(0xFFEFF6FF)
private val AdminBlueLt  = Color(0xFFDBEAFE)
private val AdminAmber   = Color(0xFFD97706)
private val PageBgTop    = Color(0xFFEFF6FF)
private val PageBgBot    = Color(0xFFF8FAFC)
private val TextPrimary  = Color(0xFF0F172A)
private val TextMuted    = Color(0xFF64748B)
private val CardBg       = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminName:  String,
    adminEmail: String,
    token:      String,
    viewModel:  AdminViewModel,
    onNavigateToUserManagement:      () -> Unit,
    onNavigateToUserProfiles:        () -> Unit,
    onNavigateToSecurityMonitor:     () -> Unit,
    onNavigateToScanRecords:         () -> Unit,
    onNavigateToFlaggedLinks:        () -> Unit,
    onNavigateToAuditLog:            () -> Unit,
    onNavigateToSubscriptions:       () -> Unit,
    onNavigateToSettings:            () -> Unit,
    onLogout: () -> Unit
) {
    val statsState by viewModel.stats.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadStats(token) }

    val initials = adminName.trim().split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercaseChar().toString() }

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
            Spacer(modifier = Modifier.height(48.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Admin Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AdminBlue)
                    Text("LinkScanner", fontSize = 13.sp, color = TextMuted)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = AdminAmber.copy(alpha = 0.15f)) {
                    Text(
                        "ADMIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Admin Profile Card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(AdminBlueLt),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials.ifBlank { "A" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AdminBlue)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(adminName.ifBlank { "Admin" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (adminEmail.isNotBlank()) {
                            Text(adminEmail, fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Stats Cards ───────────────────────────────────────────────────
            when (val s = statsState) {
                is AdminViewModel.UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminBlue, modifier = Modifier.size(28.dp))
                    }
                }
                is AdminViewModel.UiState.Success -> {
                    StatsGrid(stats = s.data)
                }
                else -> {
                    StatsGrid(stats = AdminStatsResponse(0, 0, 0, 0))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Section label ─────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Admin Features", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            }
            Spacer(modifier = Modifier.height(10.dp))

            // ── Navigation Items ──────────────────────────────────────────────
            AdminNavRow("User Management",      Icons.Default.Group,          "Create, view, update & suspend users",       onClick = onNavigateToUserManagement)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("User Profiles",        Icons.Default.ManageAccounts, "Manage roles and permissions",               onClick = onNavigateToUserProfiles)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("Security Monitor",     Icons.Default.Security,       "Failed logins & locked accounts",            onClick = onNavigateToSecurityMonitor)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("Scan Records",         Icons.Default.History,        "All user scan activity",                     onClick = onNavigateToScanRecords)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("Flagged Links",        Icons.Default.Flag,           "Malicious & suspicious links",               onClick = onNavigateToFlaggedLinks)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("Audit Log",            Icons.Default.EventNote,      "History of admin actions",                   onClick = onNavigateToAuditLog)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("Subscriptions",        Icons.Default.CreditCard,     "Assign, update & cancel user plans",         onClick = onNavigateToSubscriptions)
            Spacer(modifier = Modifier.height(8.dp))
            AdminNavRow("Settings",             Icons.Default.Settings,       "Profile, password & auto-logout",            onClick = onNavigateToSettings)

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            // ── Log Out ───────────────────────────────────────────────────────
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminBlue)
            ) {
                Icon(Icons.Default.Logout, null, tint = AdminBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AdminBlue)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatsGrid(stats: AdminStatsResponse) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total Users",   stats.totalUsers.toString(),  Color(0xFF2563EB), Color(0xFFDBEAFE), Icons.Default.People,      Modifier.weight(1f))
            StatCard("Scans Today",   stats.scansToday.toString(),  Color(0xFF16A34A), Color(0xFFDCFCE7), Icons.Default.QrCodeScanner, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Flagged Links", stats.flaggedLinks.toString(), Color(0xFFDC2626), Color(0xFFFEE2E2), Icons.Default.Warning,     Modifier.weight(1f))
            StatCard("Paid Users",    stats.paidUsers.toString(),    Color(0xFF7C3AED), Color(0xFFEDE9FE), Icons.Default.Star,         Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    label:    String,
    value:    String,
    color:    Color,
    bgColor:  Color,
    icon:     ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun AdminNavRow(
    label:       String,
    icon:        ImageVector,
    description: String,
    onClick:     () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(AdminBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = AdminBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(description, fontSize = 12.sp, color = TextMuted)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
