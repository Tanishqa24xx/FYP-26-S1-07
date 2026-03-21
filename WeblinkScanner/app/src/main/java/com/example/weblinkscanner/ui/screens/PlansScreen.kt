package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun PlansScreen(
    viewModel:      PlanViewModel,
    onUpgradeClick: (String) -> Unit,
    onBack:         () -> Unit
) {
    val myPlan  by viewModel.myPlan.collectAsState()
    val plans   by viewModel.allPlans.collectAsState()
    val loading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyPlan()
        viewModel.loadAllPlans()
    }

    val currentPlanName = myPlan?.planDetails?.name?.lowercase() ?: "free"

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
                Icon(Icons.Default.Star, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Subscription Plans", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text("Choose the plan that suits you", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(24.dp))

            if (loading) {
                CircularProgressIndicator(color = Blue600)
                Spacer(Modifier.height(24.dp))
            }

            plans.forEach { plan ->
                val isPremium  = plan.name.lowercase() == "premium"
                val isStandard = plan.name.lowercase() == "standard"
                val isCurrent  = plan.name.lowercase() == currentPlanName
                val accentColor = when {
                    isPremium  -> Blue600
                    isStandard -> Blue600
                    else       -> Color(0xFF64748B)
                }
                val accentBg = when {
                    isPremium  -> Blue50
                    isStandard -> Blue50
                    else       -> Color(0xFFF1F5F9)
                }

                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isCurrent) Modifier.border(2.dp, GreenPass, RoundedCornerShape(16.dp))
                            else Modifier.border(2.dp, Blue600, RoundedCornerShape(16.dp))
                        ),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // Plan header
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(10.dp), color = accentBg) {
                                    Icon(
                                        if (isPremium) Icons.Default.WorkspacePremium
                                        else if (isStandard) Icons.Default.Star
                                        else Icons.Default.Shield,
                                        null,
                                        tint     = accentColor,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(plan.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(plan.scanLimit, fontSize = 12.sp, color = TextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(plan.price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                if (isCurrent) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = GreenBg) {
                                        Text("Current", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = GreenPass, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                if (isPremium && !isCurrent) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Blue100) {
                                        Text("Best Value", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = Blue600, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 12.dp))

                        // Features
                        plan.features.forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.padding(vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = GreenPass, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(feature, fontSize = 12.sp, color = TextMuted)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Action button
                        if (isCurrent) {
                            OutlinedButton(
                                onClick  = {},
                                enabled  = false,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape    = RoundedCornerShape(12.dp),
                                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = GreenPass, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Current Plan", color = GreenPass, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick  = { onUpgradeClick(plan.name.lowercase()) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Choose ${plan.name}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

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
