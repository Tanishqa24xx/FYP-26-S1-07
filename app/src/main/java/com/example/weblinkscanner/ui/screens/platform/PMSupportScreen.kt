package com.example.weblinkscanner.ui.screens.platform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.PMSupportRequest
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
fun PMSupportScreen(
    token:          String,
    viewModel:      PlatformViewModel,
    onRequestClick: (String) -> Unit,
    onBack:         () -> Unit
) {
    val supportState by viewModel.supportRequests.collectAsState()
    var selectedTab  by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Open", "In Progress", "Resolved")
    val statusFilter = when (selectedTab) { 1 -> "open"; 2 -> "in_progress"; 3 -> "resolved"; else -> null }

    LaunchedEffect(selectedTab) { viewModel.loadSupportRequests(token, statusFilter) }

    Scaffold(
        containerColor = PMPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Support Requests", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.loadSupportRequests(token, statusFilter) }) { Icon(Icons.Default.Refresh, null, tint = PMGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = PMCardBg, contentColor = PMGreen, edgePadding = 0.dp) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                    text = { Text(t, fontSize = 13.sp, fontWeight = FontWeight.Medium) }) }
            }
            when (val s = supportState) {
                is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
                is PlatformViewModel.UiState.Error   -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.message, color = PMRed) }
                is PlatformViewModel.UiState.Success -> {
                    if (s.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.HeadsetMic, null, tint = PMMuted, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No support requests", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PMTxt)
                                Text("All clear!", fontSize = 13.sp, color = PMMuted)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(s.data) { req -> SupportRequestCard(req, onClick = { onRequestClick(req.id) }) }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SupportRequestCard(req: PMSupportRequest, onClick: () -> Unit) {
    val (statusColor, statusBg, statusLabel) = when (req.status) {
        "open"        -> Triple(PMTeal,  PMTealBg,  "Open")
        "in_progress" -> Triple(PMAmber, PMAmberBg, "In Progress")
        "resolved"    -> Triple(PMGreen, PMGreenBg, "Resolved")
        else          -> Triple(PMMuted, Color(0xFFF1F5F9), req.status)
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(req.subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMTxt, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text(req.userEmail ?: "Unknown user", fontSize = 12.sp, color = PMMuted)
            Text(req.message.take(100) + if (req.message.length > 100) "…" else "", fontSize = 12.sp, color = PMMuted)
            Text(req.createdAt?.take(10) ?: "", fontSize = 11.sp, color = PMMuted)
        }
    }
}
