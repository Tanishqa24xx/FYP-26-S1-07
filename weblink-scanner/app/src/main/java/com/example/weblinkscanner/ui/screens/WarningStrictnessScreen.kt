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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.utils.WarningStrictnessManager

private val Blue600   = Color(0xFF2563EB)
private val Blue50    = Color(0xFFEFF6FF)
private val Blue100   = Color(0xFFDBEAFE)
private val PageBgTop = Color(0xFFEFF6FF)
private val PageBgBot = Color(0xFFF8FAFC)
private val CardBg    = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val GreenSave   = Color(0xFF16A34A)
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)
private val RedDanger   = Color(0xFFDC2626)
private val RedBg       = Color(0xFFFEE2E2)

@Composable
fun WarningStrictnessScreen(
    userId: String = "default",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(WarningStrictnessManager.get(context, userId)) }
    var saved    by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.Tune, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("Warning Strictness", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Spacer(Modifier.height(4.dp))
            Text(
                "Control how sensitive the app is when marking URLs as risky.",
                fontSize = 13.sp, color = TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // --- Options ---
            listOf(
                Triple(
                    WarningStrictnessManager.LOW,
                    "Low",
                    "Only DANGEROUS URLs trigger a warning. SUSPICIOUS links are shown as SAFE. " +
                            "Best if you scan many links and want fewer false alarms."
                ),
                Triple(
                    WarningStrictnessManager.MEDIUM,
                    "Medium (Default)",
                    "SUSPICIOUS and DANGEROUS URLs both trigger a warning. " +
                            "Balanced - recommended for most users."
                ),
                Triple(
                    WarningStrictnessManager.HIGH,
                    "High",
                    "SUSPICIOUS and DANGEROUS URLs trigger warnings. " +
                            "Additional notices for unusual URL patterns even when the overall verdict is safe. " +
                            "Best for security-conscious users."
                ),
            ).forEach { (level, label, description) ->
                val isSelected = selected == level
                val accentColor = when (level) {
                    WarningStrictnessManager.LOW    -> GreenSave
                    WarningStrictnessManager.HIGH   -> RedDanger
                    else                            -> Blue600
                }
                val accentBg = when (level) {
                    WarningStrictnessManager.LOW    -> Color(0xFFF0FDF4)
                    WarningStrictnessManager.HIGH   -> RedBg
                    else                            -> Blue50
                }

                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = if (isSelected) accentBg else CardBg
                    ),
                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
                    border    = if (isSelected)
                        androidx.compose.foundation.BorderStroke(2.dp, accentColor)
                    else null,
                    onClick   = { selected = level; saved = false }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick  = { selected = level; saved = false },
                            colors   = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) accentColor else TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                description,
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // --- Current effect info ---
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = AmberBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (selected) {
                            WarningStrictnessManager.LOW  ->
                                "LOW: SUSPICIOUS links will be displayed as SAFE. Only confirmed dangerous URLs are flagged."
                            WarningStrictnessManager.HIGH ->
                                "HIGH: All suspicious signals are surfaced, including borderline cases."
                            else ->
                                "MEDIUM: Standard behaviour - SUSPICIOUS and DANGEROUS both show warnings."
                        },
                        fontSize = 12.sp, color = AmberWarn, lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    WarningStrictnessManager.save(context, selected, userId)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (saved) GreenSave else Blue600
                )
            ) {
                Icon(
                    if (saved) Icons.Default.CheckCircle else Icons.Default.Save,
                    null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved!" else "Save", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}