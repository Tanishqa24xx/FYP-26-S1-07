package com.example.weblinkscanner.ui.screens.platform

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenBg = Color(0xFFECFDF5)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMPurple   = Color(0xFF7C3AED); private val PMPurpleBg= Color(0xFFF3E8FF)
private val PMTeal     = Color(0xFF0891B2); private val PMTealBg  = Color(0xFFE0F2FE)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMReportsScreen(
    token:     String,
    viewModel: PlatformViewModel,
    onBack:    () -> Unit
) {
    val reportState by viewModel.reportData.collectAsState()
    val context     = LocalContext.current

    var startDate by remember { mutableStateOf("") }
    var endDate   by remember { mutableStateOf("") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    // DatePicker states
    val startPickerState = rememberDatePickerState()
    val endPickerState   = rememberDatePickerState()

    fun Long.toDateString(): String {
        val c = java.util.Calendar.getInstance().also { it.timeInMillis = this }
        return "%04d-%02d-%02d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
    }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { startDate = it.toDateString() }
                    showStartPicker = false
                }) { Text("OK", color = PMGreen) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = startPickerState) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { endDate = it.toDateString() }
                    showEndPicker = false
                }) { Text("OK", color = PMGreen) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = endPickerState) }
    }

    Scaffold(
        containerColor = PMPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Date range selector
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Date Range", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (startDate.isBlank()) "From Date" else startDate, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (endDate.isBlank()) "To Date" else endDate, fontSize = 13.sp)
                        }
                    }
                    val isLoading = reportState is PlatformViewModel.UiState.Loading
                    Button(
                        onClick = {
                            if (startDate.isNotBlank() && endDate.isNotBlank()) {
                                viewModel.generateReport(token, startDate, endDate)
                            }
                        },
                        enabled  = startDate.isNotBlank() && endDate.isNotBlank() && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PMGreen)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Generate Report", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Report results
            when (val r = reportState) {
                is PlatformViewModel.UiState.Success -> {
                    val report = r.data
                    // Export button
                    OutlinedButton(
                        onClick = {
                            val csv = buildString {
                                appendLine("Weblink Scanner Report: ${report.period["start"]} to ${report.period["end"]}")
                                appendLine("Total Scans,${report.totalScans}")
                                appendLine("New Users,${report.newUsers}")
                                appendLine()
                                appendLine("Date,Scans")
                                report.scansByDate.forEach { (d, c) -> appendLine("$d,$c") }
                            }
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, csv); putExtra(Intent.EXTRA_SUBJECT, "LinkScanner Report") },
                                "Export Report"
                            ))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = PMGreen)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export as CSV")
                    }

                    // Summary cards
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReportSummaryCard("Total Scans", report.totalScans.toString(), PMGreen, PMGreenBg, Modifier.weight(1f))
                        ReportSummaryCard("New Users",   report.newUsers.toString(),   PMTeal,  PMTealBg,  Modifier.weight(1f))
                    }

                    // Scans by date
                    if (report.scansByDate.isNotEmpty()) {
                        Text("Scans by Date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val maxScans = report.scansByDate.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                                report.scansByDate.entries.toList().takeLast(14).forEach { (date, count) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(date, fontSize = 11.sp, color = PMMuted, modifier = Modifier.width(90.dp))
                                        Box(Modifier.weight(1f).height(16.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))) {
                                            Box(Modifier.fillMaxWidth(count.toFloat() / maxScans).fillMaxHeight().background(PMGreen, RoundedCornerShape(4.dp)))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(count.toString(), fontSize = 11.sp, color = PMGreen, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Verdict breakdown
                    if (report.verdictBreakdown.isNotEmpty()) {
                        Text("Verdict Breakdown", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val vColors = mapOf("SAFE" to PMGreen, "SUSPICIOUS" to PMAmber, "DANGEROUS" to PMRed)
                                report.verdictBreakdown.forEach { (v, c) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(v, fontSize = 13.sp, color = PMTxt)
                                        Text(c.toString(), fontSize = 13.sp, color = vColors[v] ?: PMMuted, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                is PlatformViewModel.UiState.Error -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PMRedBg)) {
                        Text(r.message, color = PMRed, modifier = Modifier.padding(14.dp))
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReportSummaryCard(label: String, value: String, color: Color, bg: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = PMMuted)
        }
    }
}
