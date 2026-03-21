package com.example.weblinkscanner.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.utils.TokenManager

// --- Colors: identical to LoginScreen & ScannerScreen ---
private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)

@Composable
fun MenuScreen(
    userName: String = "",
    userEmail: String = "",
    userPlan: String = "FREE",
    onNavigateToScan: () -> Unit = {},
    onNavigateToMyPlan: () -> Unit = {},
    onNavigateToScanHistory: () -> Unit = {},
    onNavigateToSavedLinks: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    // Derive initials only when we have a real name (e.g. "Thar Lynn Htet" → "TL")
    val initials = userName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    val hasName = initials.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(PageBgTop, PageBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                Text(text = "🔗", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "LinkScanner",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Blue600
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your account menu",
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- Account Info Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- Avatar: initials if name loaded, Person icon otherwise ---
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Blue100),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasName) {
                            // Show derived initials once name is fetched from Supabase
                            Text(
                                text = initials,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue600
                            )
                        } else {
                            // Fallback to the Person icon (same as original)
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        // --- Name (from Supabase users table) ---
                        Text(
                            text = userName.ifBlank { userEmail.ifBlank { "User" } },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        // Show email only if we have a name to show above it
                        if (hasName && userEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userEmail,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // --- Plan badge ---
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Blue50
                        ) {
                            Text(
                                text = userPlan.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue600,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Navigation Menu Items ---
            MenuRow(
                label = "Scan",
                icon = Icons.Default.Search,
                description = "Check a link for threats",
                onClick = onNavigateToScan
            )
            Spacer(modifier = Modifier.height(10.dp))
            MenuRow(
                label = "My Plan",
                icon = Icons.Default.Star,
                description = "View or upgrade your plan",
                onClick = onNavigateToMyPlan
            )
            Spacer(modifier = Modifier.height(10.dp))
            MenuRow(
                label = "Scan History",
                icon = Icons.Default.History,
                description = "See your past scans",
                onClick = onNavigateToScanHistory
            )
            Spacer(modifier = Modifier.height(10.dp))
            MenuRow(
                label = "Saved Links",
                icon = Icons.Default.Bookmark,
                description = "Links you've bookmarked",
                onClick = onNavigateToSavedLinks
            )
            Spacer(modifier = Modifier.height(10.dp))
            MenuRow(
                label = "Settings",
                icon = Icons.Default.Settings,
                description = "App preferences",
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(color = DividerCol, thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // --- Log Out Button ---
            OutlinedButton(
                onClick = {
                    TokenManager.clearToken(context)
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Blue600,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue600
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Log out will end your session on this device.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp)
            )
        }
    }
}

// --- Reusable menu row ---
@Composable
private fun MenuRow(
    label: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Blue50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Blue600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}