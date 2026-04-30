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
import com.example.weblinkscanner.utils.AutoLogoutManager

private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val GreenSave   = Color(0xFF16A34A)

private val TIMEOUT_OPTIONS = listOf(5, 10, 30)

@Composable
fun AutoLogoutScreen(userId: String = "default", onBack: () -> Unit = {}) {
    val context = LocalContext.current

    // Load saved timeout on entry
    var selectedTimeout by remember {
        mutableStateOf(AutoLogoutManager.getTimeout(context, userId))
    }
    var saved by remember { mutableStateOf(false) }

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
                    .background(
                        Brush.radialGradient(listOf(Blue100, Blue50)),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("Auto Logout", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Spacer(Modifier.height(24.dp))

            // Main card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Info text
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Security, null,
                            tint     = Blue600,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "For your safety, the app logs you out after inactivity.",
                            fontSize   = 13.sp,
                            color      = TextMuted,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = DividerCol)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Timeout: $selectedTimeout minutes (${if (selectedTimeout == 10) "default" else "custom"})",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Choose timeout:", fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(10.dp))

                    // Timeout buttons
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TIMEOUT_OPTIONS.forEach { minutes ->
                            val isSelected = selectedTimeout == minutes
                            if (isSelected) {
                                Button(
                                    onClick  = { selectedTimeout = minutes; saved = false },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape    = RoundedCornerShape(10.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
                                ) {
                                    Text(
                                        "$minutes min",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick  = { selectedTimeout = minutes; saved = false },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape    = RoundedCornerShape(10.dp),
                                    border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                                ) {
                                    Text("$minutes min", fontSize = 13.sp, color = Blue600)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = DividerCol)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "When timeout happens:",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("• You return to Log in screen", fontSize = 13.sp, color = TextMuted)
                    Text("• Unsaved inputs are cleared", fontSize = 13.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    AutoLogoutManager.saveTimeout(context, selectedTimeout, userId)
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
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (saved) "Saved!" else "Save",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
