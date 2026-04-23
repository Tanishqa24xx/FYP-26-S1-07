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
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenBg = Color(0xFFECFDF5)
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
fun PMAnalyticsScreen(
    token:     String,
    viewModel: PlatformViewModel,
    onBack:    () -> Unit
) {
    val overviewState by viewModel.analyticsOverview.collectAsState()
    val featureState  by viewModel.featureAnalytics.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadAnalyticsOverview(token); viewModel.loadFeatureAnalytics(token) }

    Scaffold(
        containerColor = PMPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.loadAnalyticsOverview(token); viewModel.loadFeatureAnalytics(token) }) { Icon(Icons.Default.Refresh, null, tint = PMGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overview stats
            when (val s = overviewState) {
                is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
                is PlatformViewModel.UiState.Success -> {
                    val o = s.data
                    Text("Usage Overview", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnalyticsStatCard("Total Users",  o.totalUsers.toString(),    Icons.Default.People,        PMTeal,  PMTealBg,  Modifier.weight(1f))
                        AnalyticsStatCard("Active Users", o.activeUsers.toString(),   Icons.Default.HowToReg,      PMGreen, PMGreenBg, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnalyticsStatCard("Total Scans",   o.totalScans.toString(),       Icons.Default.QrCodeScanner, PMPurple, PMPurpleBg, Modifier.weight(1f))
                        AnalyticsStatCard("Scans Today",   o.scansToday.toString(),       Icons.Default.Today,         PMAmber,  PMAmberBg,  Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnalyticsStatCard("This Month",    o.scansThisMonth.toString(),   Icons.Default.CalendarMonth, PMTeal,  PMTealBg,  Modifier.weight(1f))
                        AnalyticsStatCard("New Today",     o.newUsersToday.toString(),    Icons.Default.PersonAdd,     PMGreen, PMGreenBg, Modifier.weight(1f))
                    }

                    // Plan distribution
                    if (o.planDistribution.isNotEmpty()) {
                        Text("Plan Distribution", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val total = o.planDistribution.values.sum().coerceAtLeast(1)
                                val planColors = mapOf("free" to PMTeal, "standard" to PMAmber, "premium" to PMPurple)
                                o.planDistribution.forEach { (plan, count) ->
                                    val color = planColors[plan.lowercase()] ?: PMGreen
                                    val pct   = (count.toFloat() / total)
                                    Column {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(plan.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PMTxt)
                                            Text("$count (${(pct * 100).toInt()}%)", fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))) {
                                            Box(Modifier.fillMaxWidth(pct).height(8.dp).background(color, RoundedCornerShape(4.dp)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is PlatformViewModel.UiState.Error -> Text(s.message, color = PMRed)
                else -> {}
            }

            // Feature / verdict analytics
            when (val f = featureState) {
                is PlatformViewModel.UiState.Success -> {
                    Text("Scan Verdict Breakdown", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val verdictColors = mapOf("SAFE" to PMGreen, "SUSPICIOUS" to PMAmber, "DANGEROUS" to PMRed)
                            val total = f.data.verdictBreakdown.values.sum().coerceAtLeast(1)
                            f.data.verdictBreakdown.forEach { (verdict, count) ->
                                val color = verdictColors[verdict] ?: PMMuted
                                val pct   = (count.toFloat() / total)
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(verdict.replaceFirstChar { it.uppercase() }.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PMTxt)
                                        Text("$count (${(pct * 100).toInt()}%)", fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))) {
                                        Box(Modifier.fillMaxWidth(pct).height(8.dp).background(color, RoundedCornerShape(4.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnalyticsStatCard(label: String, value: String, icon: ImageVector, color: Color, bg: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = PMMuted)
        }
    }
}
