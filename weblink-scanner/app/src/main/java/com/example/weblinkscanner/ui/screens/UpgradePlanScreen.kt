package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
private val RedFail     = Color(0xFFDC2626)

private data class PlanOption(
    val id: String,
    val name: String,
    val price: String,
    val color: Color,
    val bg: Color,
    val features: List<String>
)

private val PLAN_OPTIONS = listOf(
    PlanOption(
        id    = "free",
        name  = "Free",
        price = "\$0/month",
        color = Color(0xFF64748B),
        bg    = Color(0xFFF1F5F9),
        features = listOf(
            "5 scans/day",
            "Manual URL scanning",
            "Camera OCR scanning",
            "Basic Risk Classification",
            "Save Important Links",
            "Last 5 scans in history"
        )
    ),
    PlanOption(
        id    = "standard",
        name  = "Standard",
        price = "\$4.99/month",
        color = Blue600,
        bg    = Blue50,
        features = listOf(
            "Unlimited scans",
            "Detailed Risk Classification",
            "Detailed Security Analysis",
            "Alert Threshold Notification",
            "Last 30 days scan history",
            "Export history (CSV)"
        )
    ),
    PlanOption(
        id    = "premium",
        name  = "Premium",
        price = "\$9.99/month",
        color = Blue600,
        bg    = Blue50,
        features = listOf(
            "Everything in Standard",
            "Advanced Multi-layer Analysis",
            "Full scan history",
            "Export history (CSV + PDF)",
            "Ad-heavy website warnings",
            "Script & tracker detection"
        )
    )
)

@Composable
fun UpgradePlanScreen(
    viewModel: PlanViewModel,
    userId: String = "00000000-0000-0000-0000-000000000000",
    preSelectedPlan: String = "standard",
    onBack: () -> Unit
) {
    val myPlan by viewModel.myPlan.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    var selectedPlan by remember { mutableStateOf(preSelectedPlan) }
    var confirmed by remember { mutableStateOf(false) }

    val currentPlan = myPlan?.currentPlan?.uppercase() ?: "FREE"
    val selectedOption = PLAN_OPTIONS.find { it.id == selectedPlan } ?: PLAN_OPTIONS[0]

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

            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowUpward, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Change Plan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text("Currently on $currentPlan plan", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(24.dp))

            // Error
            error?.let {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = RedFail, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = RedFail, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Plan selection
            Text("Select your plan:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = TextPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(10.dp))

            PLAN_OPTIONS.forEach { option ->
                val isSelected = selectedPlan == option.id
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPlan = option.id; confirmed = false }
                        .then(
                            if (isSelected) Modifier.border(2.dp, option.color, RoundedCornerShape(16.dp))
                            else Modifier
                        ),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = if (isSelected) option.bg else CardBg
                    ),
                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick  = { selectedPlan = option.id; confirmed = false },
                                    colors   = RadioButtonDefaults.colors(selectedColor = option.color)
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text(option.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                        color = if (isSelected) option.color else TextPrimary)
                                    Text(option.price, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) option.color else TextMuted)
                                }
                            }
                            if (option.id == "premium") {
                                Surface(shape = RoundedCornerShape(6.dp), color = Blue100) {
                                    Text("Best Value", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = Blue600,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }

                        if (isSelected) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = option.color.copy(alpha = 0.2f))
                            Spacer(Modifier.height(10.dp))
                            option.features.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = GreenPass, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(feature, fontSize = 12.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Summary card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Order Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    SummaryRow("Plan",    selectedOption.name)
                    HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                    SummaryRow("Price",   selectedOption.price)
                    HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                    SummaryRow("Billing", "Monthly")
                    HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 8.dp))
                    SummaryRow("Starts",  "Today")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Confirm button
            Button(
                onClick = {
                    viewModel.upgradePlan(selectedPlan, userId)
                    confirmed = true
                },
                enabled  = !loading && !confirmed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = if (confirmed) GreenPass else selectedOption.color,
                    disabledContainerColor = if (confirmed) GreenPass else DividerCol
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else if (confirmed) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Plan Changed!", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm: ${selectedOption.name} Plan", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text(if (confirmed) "Done" else "Cancel",
                    fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "You can change your plan at any time.",
                fontSize = 12.sp,
                color    = TextMuted
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
