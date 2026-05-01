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
import com.example.weblinkscanner.data.models.SubscriptionUser
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val SubBlue    = Color(0xFF1D4ED8); private val SubBlueLt  = Color(0xFFDBEAFE); private val SubBlueBg = Color(0xFFEFF6FF)
private val SubGreen   = Color(0xFF16A34A); private val SubGreenBg = Color(0xFFDCFCE7)
private val SubAmber   = Color(0xFFD97706); private val SubAmberBg = Color(0xFFFEF3C7)
private val SubPurple  = Color(0xFF7C3AED); private val SubPurpleBg = Color(0xFFF3E8FF)
private val SubTxt     = Color(0xFF0F172A)
private val SubMuted   = Color(0xFF64748B)
private val SubPageBg  = Color(0xFFF1F5F9)
private val SubCardBg  = Color.White

private fun planColor(plan: String?): Pair<Color, Color> = when (plan?.lowercase()) {
    "premium"  -> SubPurple to SubPurpleBg
    "standard" -> SubAmber  to SubAmberBg
    else       -> SubBlue   to SubBlueBg
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagementScreen(
    token:        String,
    viewModel:    AdminViewModel,
    onUserClick:  (String) -> Unit,
    onBack:       () -> Unit
) {
    val subState   = viewModel.subscriptions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterPlan  by remember { mutableStateOf("All") }

    val planFilters = listOf("All", "Free", "Standard", "Premium")

    LaunchedEffect(Unit) { viewModel.loadSubscriptions(token) }

    Scaffold(
        containerColor = SubPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.loadSubscriptions(token) }) {
                        Icon(Icons.Default.Refresh, null, tint = SubBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SubCardBg, titleContentColor = SubTxt)
            )
        }
    ) { padding ->
        when (val s = subState.value) {
            is AdminViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SubBlue)
                }
            }
            is AdminViewModel.UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color(0xFFDC2626))
                }
            }
            is AdminViewModel.UiState.Success -> {
                val stats = s.data.stats
                val allUsers = s.data.users

                val filtered = allUsers.filter { user ->
                    val isRegularUser = user.role?.lowercase() !in listOf("admin", "platform_manager")
                    val matchesPlan   = filterPlan == "All" || user.plan?.equals(filterPlan, ignoreCase = true) == true
                    val matchesSearch = searchQuery.isBlank() ||
                            user.name?.contains(searchQuery, ignoreCase = true) == true ||
                            user.email?.contains(searchQuery, ignoreCase = true) == true
                    isRegularUser && matchesPlan && matchesSearch
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Stats cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubStatCard("Total", stats.total, SubBlue, SubBlueBg, Modifier.weight(1f))
                            SubStatCard("Free",  stats.free,  SubGreen, SubGreenBg, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubStatCard("Standard", stats.standard, SubAmber,  SubAmberBg,  Modifier.weight(1f))
                            SubStatCard("Premium",  stats.premium,  SubPurple, SubPurpleBg, Modifier.weight(1f))
                        }
                    }

                    // Search
                    item {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier      = Modifier.fillMaxWidth(),
                            placeholder   = { Text("Search users\u2026", color = SubMuted) },
                            leadingIcon   = { Icon(Icons.Default.Search, null, tint = SubMuted) },
                            trailingIcon  = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = SubMuted)
                                    }
                                }
                            },
                            singleLine    = true,
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = SubBlue,
                                unfocusedBorderColor    = Color(0xFFE2E8F0),
                                focusedContainerColor   = SubCardBg,
                                unfocusedContainerColor = SubCardBg
                            )
                        )
                    }

                    // Plan filter chips
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            planFilters.forEach { plan ->
                                FilterChip(
                                    selected = filterPlan == plan,
                                    onClick  = { filterPlan = plan },
                                    label    = { Text(plan, fontSize = 12.sp) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor    = SubBlue,
                                        selectedLabelColor        = Color.White
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Text("${filtered.size} user${if (filtered.size != 1) "s" else ""}",
                            fontSize = 13.sp, color = SubMuted)
                    }

                    // User list
                    items(filtered) { user ->
                        SubUserRow(user = user, onClick = { onUserClick(user.id) })
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun SubStatCard(label: String, count: Int, color: Color, bg: Color, modifier: Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = SubCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(count.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 13.sp, color = SubMuted)
        }
    }
}

@Composable
private fun SubUserRow(user: SubscriptionUser, onClick: () -> Unit) {
    val (planColor, planBg) = planColor(user.plan)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SubCardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = (user.name ?: user.email ?: "?")
                .trim().split(" ").filter { it.isNotBlank() }
                .take(2).joinToString("") { it.first().uppercaseChar().toString() }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(planBg),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = planColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name ?: "No Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SubTxt)
                Text(user.email ?: "", fontSize = 12.sp, color = SubMuted)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = planBg) {
                Text(
                    (user.plan ?: "free").replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = planColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
