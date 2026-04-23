package com.example.weblinkscanner.ui.screens.admin

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.AdminScanRecord
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val SRBlue   = Color(0xFF1D4ED8)
private val SRGreen  = Color(0xFF16A34A); private val SRGreenBg  = Color(0xFFDCFCE7)
private val SRAmber  = Color(0xFFD97706); private val SRAmberBg  = Color(0xFFFEF3C7)
private val SRRed    = Color(0xFFDC2626); private val SRRedBg    = Color(0xFFFEE2E2)
private val SRTxt    = Color(0xFF0F172A)
private val SRMuted  = Color(0xFF64748B)
private val SRPageBg = Color(0xFFF1F5F9)
private val SRCardBg = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanRecordsScreen(
    token:     String,
    viewModel: AdminViewModel,
    onBack:    () -> Unit
) {
    val scanState = viewModel.scanRecords.collectAsState()
    val context   = LocalContext.current

    val tabs      = listOf("All", "Dangerous", "Suspicious", "Safe")
    var selectedTab by remember { mutableStateOf(0) }

    val verdictFilter = when (selectedTab) {
        1    -> "DANGEROUS"
        2    -> "SUSPICIOUS"
        3    -> "SAFE"
        else -> null
    }

    LaunchedEffect(selectedTab) { viewModel.loadScanRecords(token, verdictFilter) }

    Scaffold(
        containerColor = SRPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Scan Records", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    // Export/share as CSV
                    if (scanState.value is AdminViewModel.UiState.Success) {
                        val records = (scanState.value as AdminViewModel.UiState.Success).data.records
                        IconButton(onClick = {
                            val csv = buildString {
                                appendLine("URL,Verdict,Risk Score,User Email,Scanned At")
                                records.forEach { r ->
                                    appendLine("\"${r.url ?: ""}\",${r.verdict ?: ""},${r.riskScore ?: ""},${r.userEmail ?: ""},${r.scannedAt ?: ""}")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "Scan Records Export")
                            }
                            context.startActivity(Intent.createChooser(intent, "Export CSV"))
                        }) {
                            Icon(Icons.Default.Share, null, tint = SRBlue)
                        }
                    }
                    IconButton(onClick = { viewModel.loadScanRecords(token, verdictFilter) }) {
                        Icon(Icons.Default.Refresh, null, tint = SRBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SRCardBg, titleContentColor = SRTxt)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = SRCardBg,
                contentColor     = SRBlue,
                edgePadding      = 0.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            when (val s = scanState.value) {
                is AdminViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SRBlue)
                    }
                }
                is AdminViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = SRRed)
                    }
                }
                is AdminViewModel.UiState.Success -> {
                    val records = s.data.records
                    Column {
                        // Total count bar
                        Surface(color = SRCardBg) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null, tint = SRMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${records.size} record${if (records.size != 1) "s" else ""} found",
                                    fontSize = 13.sp, color = SRMuted)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        if (records.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No records found", color = SRMuted, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(records) { record ->
                                    ScanRecordRow(record)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ScanRecordRow(record: AdminScanRecord) {
    val (verdictColor, verdictBg) = when (record.verdict?.uppercase()) {
        "DANGEROUS"  -> SRRed   to SRRedBg
        "SUSPICIOUS" -> SRAmber to SRAmberBg
        else         -> SRGreen to SRGreenBg
    }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SRCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(6.dp), color = verdictBg) {
                    Text(
                        record.verdict ?: "UNKNOWN",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = verdictColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    record.scannedAt?.take(10) ?: "",
                    fontSize = 11.sp, color = SRMuted
                )
            }
            Text(
                record.url ?: "Unknown URL",
                fontSize = 13.sp, color = SRTxt, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(record.userEmail ?: "Unknown user", fontSize = 11.sp, color = SRMuted)
                record.riskScore?.let { score ->
                    Text("Risk: ${String.format("%.0f", score * 100)}%", fontSize = 11.sp, color = verdictColor)
                }
            }
            // Threat categories
            if (!record.threatCategories.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    record.threatCategories.take(3).forEach { cat ->
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF1F5F9)) {
                            Text(cat, fontSize = 10.sp, color = SRMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
