package com.example.weblinkscanner.ui.screens.platform

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.PMCreatePlanRequest
import com.example.weblinkscanner.data.models.PMSubscriptionPlan
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenLt = Color(0xFFD1FAE5); private val PMGreenBg = Color(0xFFECFDF5)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMTeal     = Color(0xFF0891B2); private val PMTealBg  = Color(0xFFE0F2FE)
private val PMPurple   = Color(0xFF7C3AED); private val PMPurpleBg= Color(0xFFF3E8FF)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMSubscriptionPlansScreen(
    token:       String,
    viewModel:   PlatformViewModel,
    onPlanClick: (String) -> Unit,
    onBack:      () -> Unit
) {
    val plansState  by viewModel.plans.collectAsState()
    val planAction  by viewModel.planAction.collectAsState()
    val snackbar    = remember { SnackbarHostState() }

    var showCreate  by remember { mutableStateOf(false) }
    var filterTab   by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Active", "Suspended")

    LaunchedEffect(Unit) { viewModel.loadPlans(token) }

    LaunchedEffect(planAction) {
        when (planAction) {
            is PlatformViewModel.UiState.Success -> {
                snackbar.showSnackbar((planAction as PlatformViewModel.UiState.Success<String>).data)
                viewModel.clearPlanAction(); viewModel.loadPlans(token)
            }
            is PlatformViewModel.UiState.Error -> {
                snackbar.showSnackbar((planAction as PlatformViewModel.UiState.Error).message)
                viewModel.clearPlanAction()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = PMPageBg,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Subscription Plans", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.loadPlans(token) }) { Icon(Icons.Default.Refresh, null, tint = PMGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = PMGreen, contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = filterTab, containerColor = PMCardBg, contentColor = PMGreen) {
                tabs.forEachIndexed { i, t -> Tab(selected = filterTab == i, onClick = { filterTab = i },
                    text = { Text(t, fontSize = 13.sp, fontWeight = FontWeight.Medium) }) }
            }
            when (val s = plansState) {
                is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
                is PlatformViewModel.UiState.Error   -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.message, color = PMRed) }
                is PlatformViewModel.UiState.Success -> {
                    val filtered = when (filterTab) {
                        1    -> s.data.filter { it.status == "active" }
                        2    -> s.data.filter { it.status == "suspended" }
                        else -> s.data
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No plans found", color = PMMuted) }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(filtered) { plan -> PlanCard(plan = plan, onClick = { onPlanClick(plan.id) }) }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    if (showCreate) {
        CreatePlanDialog(
            onDismiss = { showCreate = false },
            onCreate  = { req -> viewModel.createPlan(token, req) { showCreate = false } }
        )
    }
}

@Composable
private fun PlanCard(plan: PMSubscriptionPlan, onClick: () -> Unit) {
    val isSuspended = plan.status == "suspended"
    val (statusColor, statusBg) = if (!isSuspended) PMGreen to PMGreenBg else PMAmber to PMAmberBg
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PMCardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(plan.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PMTxt)
                    Text(if (plan.price == 0.0) "Free" else "$${String.format("%.2f", plan.price)}/month",
                        fontSize = 14.sp, color = PMGreen, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                    Text(plan.status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            if (!plan.description.isNullOrBlank()) Text(plan.description, fontSize = 13.sp, color = PMMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, tint = PMMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${plan.userCount ?: 0} users", fontSize = 12.sp, color = PMMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = PMMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (plan.scanLimit == 0) "Unlimited" else "${plan.scanLimit}/day", fontSize = 12.sp, color = PMMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (PMCreatePlanRequest) -> Unit) {
    var name      by remember { mutableStateOf("") }
    var desc      by remember { mutableStateOf("") }
    var price     by remember { mutableStateOf("0.00") }
    var scanLimit by remember { mutableStateOf("10") }
    var features  by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PMCardBg,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Create Plan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PMTxt) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("Plan Name *") }, isError = nameError, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Description") }, maxLines = 2,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it },
                        label = { Text("Price ($)") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = scanLimit, onValueChange = { scanLimit = it },
                        label = { Text("Scans/day") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                }
                OutlinedTextField(value = features, onValueChange = { features = it },
                    label = { Text("Features (comma-separated)") }, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    onCreate(PMCreatePlanRequest(
                        name = name.trim(), description = desc.trim(),
                        price = price.toDoubleOrNull() ?: 0.0,
                        scanLimit = scanLimit.toIntOrNull() ?: 10,
                        features = features.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = PMGreen),
                shape  = RoundedCornerShape(10.dp)
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = PMMuted) } }
    )
}
