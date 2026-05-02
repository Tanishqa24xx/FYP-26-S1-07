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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

private val FLBlue   = Color(0xFF1D4ED8)
private val FLGreen  = Color(0xFF16A34A)
private val FLAmber  = Color(0xFFD97706); private val FLAmberBg  = Color(0xFFFEF3C7)
private val FLRed    = Color(0xFFDC2626); private val FLRedBg    = Color(0xFFFEE2E2)
private val FLTxt    = Color(0xFF0F172A)
private val FLMuted  = Color(0xFF64748B)
private val FLPageBg = Color(0xFFF1F5F9)
private val FLCardBg = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlaggedLinksScreen(
    token:     String,
    viewModel: AdminViewModel,
    onBack:    () -> Unit
) {
    val flaggedState = viewModel.flaggedLinks.collectAsState()
    val context      = LocalContext.current

    val tabs = listOf("All Flagged", "Dangerous", "Suspicious")
    var selectedTab by remember { mutableStateOf(0) }

    val verdictFilter = when (selectedTab) {
        1    -> "DANGEROUS"
        2    -> "SUSPICIOUS"
        else -> null
    }

    LaunchedEffect(selectedTab) { viewModel.loadFlaggedLinks(token, verdictFilter) }

    Scaffold(
        containerColor = FLPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Flagged Links", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (flaggedState.value is AdminViewModel.UiState.Success) {
                        val records = (flaggedState.value as AdminViewModel.UiState.Success).data.records
                        IconButton(onClick = {
                            val csv = buildString {
                                appendLine("URL,Verdict,Risk Score,Threat Categories,User Email,Scanned At")
                                records.forEach { r ->
                                    appendLine("\"${r.url ?: ""}\",${r.verdict ?: ""},${r.riskScore ?: ""},\"${r.threatCategories?.joinToString("|") ?: ""}\",${r.userEmail ?: ""},${r.scannedAt ?: ""}")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "Flagged Links Export")
                            }
                            context.startActivity(Intent.createChooser(intent, "Export CSV"))
                        }) { Icon(Icons.Default.Share, null, tint = FLBlue) }
                    }
                    IconButton(onClick = { viewModel.loadFlaggedLinks(token, verdictFilter) }) {
                        Icon(Icons.Default.Refresh, null, tint = FLBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FLCardBg, titleContentColor = FLTxt)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = FLCardBg, contentColor = FLBlue) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            when (val s = flaggedState.value) {
                is AdminViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FLBlue)
                    }
                }
                is AdminViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = FLRed)
                    }
                }
                is AdminViewModel.UiState.Success -> {
                    val records = s.data.records
                    if (records.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, tint = FLGreen, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No flagged links found", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = FLTxt)
                                Text("No threats detected in this filter", fontSize = 13.sp, color = FLMuted)
                            }
                        }
                    } else {
                        Column {
                            Surface(color = FLCardBg) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Flag, null, tint = FLRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${records.size} flagged link${if (records.size != 1) "s" else ""} found",
                                        fontSize = 13.sp, color = FLRed, fontWeight = FontWeight.Medium)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(records) { record -> FlaggedLinkRow(record) }
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
private fun FlaggedLinkRow(record: AdminScanRecord) {
    val isDangerous = record.verdict?.uppercase() == "DANGEROUS"
    val (verdictColor, verdictBg) = if (isDangerous) FLRed to FLRedBg else FLAmber to FLAmberBg
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = FLCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (isDangerous) Icons.Default.Dangerous else Icons.Default.Warning,
                        null, tint = verdictColor, modifier = Modifier.size(16.dp)
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = verdictBg) {
                        Text(record.verdict ?: "FLAGGED", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = verdictColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Text(record.scannedAt?.take(10) ?: "", fontSize = 11.sp, color = FLMuted)
            }
            Text(record.url ?: "Unknown URL", fontSize = 13.sp, color = FLTxt,
                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // --- Updated Threat Categories Section ---
            if (!record.threatCategories.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Detection Reasons:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FLTxt
                    )

                    // Expand/Collapse Arrow
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = FLMuted
                        )
                    }
                }

                if (expanded) {
                    // Show all reasons
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        record.threatCategories.forEach { category ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontSize = 10.sp, color = verdictColor)
                                Text(category, fontSize = 10.sp, color = FLMuted, lineHeight = 14.sp)
                            }
                        }
                    }
                } else {
                    // Show only the first reason with ellipsis
                    Text(
                        text = record.threatCategories.first(),
                        fontSize = 10.sp,
                        color = FLMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (record.threatCategories.size > 1) {
                        Text(
                            "+${record.threatCategories.size - 1} more signal${if (record.threatCategories.size > 2) "s" else ""}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FLMuted
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(record.userEmail ?: "Unknown user", fontSize = 11.sp, color = FLMuted)
                record.riskScore?.let { score ->
                    Text("Risk: ${String.format("%.0f", score * 100)}%", fontSize = 11.sp,
                        color = verdictColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
