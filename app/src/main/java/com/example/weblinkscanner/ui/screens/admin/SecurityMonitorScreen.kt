package com.example.weblinkscanner.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.AdminUser
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val SecBlue    = Color(0xFF1D4ED8)
private val SecBlueLt  = Color(0xFFDBEAFE)
private val SecGreen   = Color(0xFF16A34A)
private val SecGreenBg = Color(0xFFDCFCE7)
private val SecAmber   = Color(0xFFD97706)
private val SecAmberBg = Color(0xFFFEF3C7)
private val SecRed     = Color(0xFFDC2626)
private val SecRedBg   = Color(0xFFFEE2E2)
private val SecTxt     = Color(0xFF0F172A)
private val SecMuted   = Color(0xFF64748B)
private val SecPageBg  = Color(0xFFF1F5F9)
private val SecCardBg  = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityMonitorScreen(
    token:     String,
    viewModel: AdminViewModel,
    onBack:    () -> Unit
) {
    val secState     by viewModel.securityUsers.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All At-Risk", "Locked")

    LaunchedEffect(Unit) { viewModel.loadSecurityUsers(token) }

    LaunchedEffect(actionResult) {
        when (actionResult) {
            is AdminViewModel.UiState.Success -> {
                snackbarHostState.showSnackbar(
                    (actionResult as AdminViewModel.UiState.Success<String>).data
                )
                viewModel.clearActionResult()
                viewModel.loadSecurityUsers(token)
            }
            is AdminViewModel.UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (actionResult as AdminViewModel.UiState.Error).message
                )
                viewModel.clearActionResult()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = SecPageBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Security Monitor", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSecurityUsers(token) }) {
                        Icon(Icons.Default.Refresh, null, tint = SecBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecCardBg, titleContentColor = SecTxt
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = SecCardBg,
                contentColor     = SecBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text     = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            when (val s = secState) {
                is AdminViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SecBlue)
                    }
                }
                is AdminViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = SecRed)
                    }
                }
                is AdminViewModel.UiState.Success -> {
                    val filtered = when (selectedTab) {
                        1    -> s.data.filter { it.accountStatus == "locked" }
                        else -> s.data
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, tint = SecGreen, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (selectedTab == 1) "No locked accounts" else "No at-risk accounts",
                                    fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SecTxt
                                )
                                Text("Everything looks good", fontSize = 13.sp, color = SecMuted)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            item {
                                // Summary card
                                Card(
                                    modifier  = Modifier.fillMaxWidth(),
                                    shape     = RoundedCornerShape(12.dp),
                                    colors    = CardDefaults.cardColors(containerColor = SecRedBg),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = SecRed, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "${filtered.size} account${if (filtered.size != 1) "s" else ""} require attention",
                                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecRed
                                        )
                                    }
                                }
                            }
                            items(filtered) { user ->
                                SecurityUserCard(
                                    user     = user,
                                    onUnlock = { viewModel.unlockUser(token, user.id) },
                                    onLock   = { viewModel.lockUser(token, user.id) }
                                )
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
private fun SecurityUserCard(
    user:     AdminUser,
    onUnlock: () -> Unit,
    onLock:   () -> Unit
) {
    val isLocked    = user.accountStatus == "locked"
    val failedCount = user.failedLoginCount ?: 0

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = SecCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            val initials = (user.name ?: user.email ?: "?")
                .trim().split(" ").filter { it.isNotBlank() }
                .take(2).joinToString("") { it.first().uppercaseChar().toString() }
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (isLocked) SecRedBg else SecAmberBg),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (isLocked) SecRed else SecAmber)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name ?: "No Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SecTxt)
                Text(user.email ?: "", fontSize = 12.sp, color = SecMuted)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Account status badge
                    val (badgeColor, badgeBg, badgeLabel) = when (user.accountStatus) {
                        "locked"    -> Triple(SecRed,   SecRedBg,   "Locked")
                        "suspended" -> Triple(SecAmber, SecAmberBg, "Suspended")
                        else        -> Triple(SecGreen, SecGreenBg, "Active")
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                        Text(badgeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    // Failed count badge
                    if (failedCount > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = SecAmberBg) {
                            Text("$failedCount failed login${if (failedCount != 1) "s" else ""}",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Action button
            if (isLocked) {
                TextButton(
                    onClick = onUnlock,
                    colors  = ButtonDefaults.textButtonColors(contentColor = SecGreen)
                ) { Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            } else if (failedCount >= 3) {
                TextButton(
                    onClick = onLock,
                    colors  = ButtonDefaults.textButtonColors(contentColor = SecRed)
                ) { Text("Lock", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
    }
}
