package com.example.weblinkscanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Colors: same palette as all other screens ---
private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val ErrorRed    = Color(0xFFDC2626)
private val ErrorRedBg  = Color(0xFFFEF2F2)

@Composable
fun SettingsScreen(
    onNavigateToEditProfile: () -> Unit        = {},
    onNavigateToAutoLogout: () -> Unit         = {},
    onNavigateToHelpFaq: () -> Unit            = {},
    onNavigateToWarningStrictness: () -> Unit  = {},
    onDeleteAccount: () -> Unit                = {},
    onBack: () -> Unit                         = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- Delete Account Confirmation Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Text("⚠️", fontSize = 32.sp) },
            title = {
                Text(
                    text       = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text      = "This will permanently delete your account and all associated data. This action cannot be undone.",
                    fontSize  = 14.sp,
                    color     = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showDeleteDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape  = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(PageBgTop, PageBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(56.dp))

            // --- Logo + Title ---
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(listOf(Blue100, Blue50)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚙️", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text       = "Settings",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = Blue600
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = "Manage your account & preferences",
                fontSize = 14.sp,
                color    = TextMuted
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Settings Items ---
            SettingsRow(
                label       = "Edit Profile",
                description = "Update your name and email",
                icon        = Icons.Default.Person,
                onClick     = onNavigateToEditProfile
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsRow(
                label       = "Auto Log Out",
                description = "Set session timeout preferences",
                icon        = Icons.Default.Timer,
                onClick     = onNavigateToAutoLogout
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsRow(
                label       = "Help / FAQ",
                description = "Get support and find answers",
                icon        = Icons.Default.HelpOutline,
                onClick     = onNavigateToHelpFaq
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsRow(
                label       = "Warning Strictness",
                description = "Set how sensitive scan warnings are",
                icon        = Icons.Default.Tune,
                onClick     = onNavigateToWarningStrictness
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Delete Account: red tinted to signal danger
            SettingsRow(
                label       = "Delete Account",
                description = "Permanently remove your account",
                icon        = Icons.Default.DeleteForever,
                onClick     = { showDeleteDialog = true },
                isDanger    = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- Divider ---
            HorizontalDivider(color = DividerCol, thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // --- Back Button ---
            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint               = Blue600,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "Back",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Blue600
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// --- Reusable settings row ---
@Composable
private fun SettingsRow(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val iconBubbleBg  = if (isDanger) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
    val iconTint      = if (isDanger) Color(0xFFDC2626) else Color(0xFF2563EB)
    val labelColor    = if (isDanger) Color(0xFFDC2626) else Color(0xFF0F172A)
    val descColor     = if (isDanger) Color(0xFFDC2626).copy(alpha = 0.7f) else Color(0xFF64748B)
    val cardBgColor   = if (isDanger) Color(0xFFFEF2F2) else Color.White

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDanger) 0.dp else 2.dp),
        border    = if (isDanger)
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
        else null,
        onClick   = onClick
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier            = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBubbleBg),
                    contentAlignment    = Alignment.Center
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = label,
                        tint               = iconTint,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text       = label,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = labelColor
                    )
                    Text(
                        text     = description,
                        fontSize = 12.sp,
                        color    = descColor
                    )
                }
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = if (isDanger) Color(0xFFDC2626).copy(alpha = 0.5f) else Color(0xFF64748B),
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}