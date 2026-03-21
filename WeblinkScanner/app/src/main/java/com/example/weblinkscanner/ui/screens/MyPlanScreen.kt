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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.viewmodel.PlanViewModel

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
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)

@Composable
fun MyPlanScreen(
    viewModel: PlanViewModel,
    userId: String = "00000000-0000-0000-0000-000000000000",
    onViewPaidPlansClick: () -> Unit,
    onBack: () -> Unit
) {
    val myPlan by viewModel.myPlan.collectAsState()
    val loading by viewModel.isLoading.collectAsState()

    // Reload every time this screen is shown
    LaunchedEffect(userId) { viewModel.loadMyPlan(userId) }

    val planName = myPlan?.currentPlan?.uppercase() ?: "FREE"
    val dailyLimit = myPlan?.dailyLimit ?: 5
    val scansToday = myPlan?.scansToday ?: 0
    val remaining = (dailyLimit - scansToday).coerceAtLeast(0)
    val isPaid = planName != "FREE"

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

            // Header icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkspacePremium, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("My Plan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Spacer(Modifier.height(24.dp))

            if (loading) {
                CircularProgressIndicator(color = Blue600)
                Spacer(Modifier.height(24.dp))
            }

            // Plan badge card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = if (isPaid) AmberBg else Blue50),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isPaid) Icons.Default.Star else Icons.Default.Shield,
                        null,
                        tint     = if (isPaid) AmberWarn else Blue600,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "$planName Plan",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (isPaid) AmberWarn else Blue600
                        )
                        Text(
                            if (isPaid) "Premium access active" else "Basic access",
                            fontSize = 13.sp,
                            color    = TextMuted
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenBg
                    ) {
                        Text(
                            "Active",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = GreenPass,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Usage card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Today's Usage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        UsageStat("Scans Used",      "$scansToday",  TextPrimary)
                        UsageStat("Daily Limit",     "$dailyLimit",  TextPrimary)
                        UsageStat("Remaining",       "$remaining",   if (remaining > 0) GreenPass else Color(0xFFDC2626))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Progress bar
                    val progress = if (dailyLimit > 0) scansToday.toFloat() / dailyLimit else 0f
                    LinearProgressIndicator(
                        progress    = { progress.coerceIn(0f, 1f) },
                        modifier    = Modifier.fillMaxWidth().height(8.dp).then(Modifier.background(Color.Transparent, RoundedCornerShape(4.dp))),
                        color       = if (progress > 0.8f) Color(0xFFDC2626) else Blue600,
                        trackColor  = DividerCol,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$scansToday of $dailyLimit scans used today", fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Benefits card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Benefits", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))

                    val benefits = myPlan?.planDetails?.features ?: listOf(
                        "Manual URL scanning",
                        "Camera OCR scanning",
                        "Basic Risk Level Classification",
                        "Save Important Links",
                        "Standard Security Analysis",
                        "Sandbox Environment"
                    )

                    benefits.forEachIndexed { index, feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = GreenPass, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(feature, fontSize = 13.sp, color = TextPrimary)
                        }
                        if (index < benefits.lastIndex) {
                            HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (!isPaid) {
                Button(
                    onClick  = onViewPaidPlansClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View Paid Plans", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Upgrading increases scan limits and unlocks more features.",
                    fontSize = 12.sp,
                    color    = TextMuted
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back", fontWeight = FontWeight.SemiBold, color = Blue600)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UsageStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}
