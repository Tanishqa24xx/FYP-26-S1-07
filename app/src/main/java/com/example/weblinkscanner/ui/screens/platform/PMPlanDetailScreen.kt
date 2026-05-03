package com.example.weblinkscanner.ui.screens.platform

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.PMUpdatePlanRequest
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenBg = Color(0xFFECFDF5)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMPlanDetailScreen(
    planId:    String,
    token:     String,
    viewModel: PlatformViewModel,
    onBack:    () -> Unit
) {
    val planState  by viewModel.selectedPlan.collectAsState()
    val planAction by viewModel.planAction.collectAsState()
    val snackbar   = remember { SnackbarHostState() }

    var isEditing      by remember { mutableStateOf(false) }
    var editName       by remember { mutableStateOf("") }
    var editDesc       by remember { mutableStateOf("") }
    var editPrice      by remember { mutableStateOf("") }
    var editScanLimit  by remember { mutableStateOf("") }
    var editFeatures   by remember { mutableStateOf("") }
    var confirmSuspend by remember { mutableStateOf(false) }
    var confirmActivate by remember { mutableStateOf(false) }

    LaunchedEffect(planId) { viewModel.loadPlan(token, planId) }

    LaunchedEffect(planState) {
        if (planState is PlatformViewModel.UiState.Success) {
            val p = (planState as PlatformViewModel.UiState.Success).data
            if (editName.isBlank()) { editName = p.name; editDesc = p.description ?: ""; editPrice = p.price.toString(); editScanLimit = p.scanLimit.toString(); editFeatures = p.features.joinToString(", ") }
        }
    }

    LaunchedEffect(planAction) {
        when (planAction) {
            is PlatformViewModel.UiState.Success -> { snackbar.showSnackbar((planAction as PlatformViewModel.UiState.Success<String>).data); viewModel.clearPlanAction(); isEditing = false }
            is PlatformViewModel.UiState.Error   -> { snackbar.showSnackbar((planAction as PlatformViewModel.UiState.Error).message); viewModel.clearPlanAction() }
            else -> {}
        }
    }

    if (confirmSuspend) AlertDialog(onDismissRequest = { confirmSuspend = false },
        shape = RoundedCornerShape(16.dp),
        icon  = { Icon(Icons.Default.Block, null, tint = PMRed, modifier = Modifier.size(36.dp)) },
        title = { Text("Suspend Plan?", fontWeight = FontWeight.Bold) },
        text  = { Text("Users can no longer subscribe to this plan.") },
        confirmButton = { Button(onClick = { viewModel.suspendPlan(token, planId); confirmSuspend = false },
            colors = ButtonDefaults.buttonColors(containerColor = PMRed), shape = RoundedCornerShape(10.dp)) { Text("Suspend") } },
        dismissButton = { TextButton(onClick = { confirmSuspend = false }) { Text("Cancel") } })

    if (confirmActivate) AlertDialog(onDismissRequest = { confirmActivate = false },
        shape = RoundedCornerShape(16.dp),
        icon  = { Icon(Icons.Default.CheckCircle, null, tint = PMGreen, modifier = Modifier.size(36.dp)) },
        title = { Text("Activate Plan?", fontWeight = FontWeight.Bold) },
        text  = { Text("This plan will be available for subscriptions.") },
        confirmButton = { Button(onClick = { viewModel.activatePlan(token, planId); confirmActivate = false },
            colors = ButtonDefaults.buttonColors(containerColor = PMGreen), shape = RoundedCornerShape(10.dp)) { Text("Activate") } },
        dismissButton = { TextButton(onClick = { confirmActivate = false }) { Text("Cancel") } })

    Scaffold(
        containerColor = PMPageBg,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Plan Detail", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (planState is PlatformViewModel.UiState.Success) {
                        IconButton(onClick = { if (isEditing) { val p = (planState as PlatformViewModel.UiState.Success).data; editName = p.name; editDesc = p.description ?: ""; editPrice = p.price.toString(); editScanLimit = p.scanLimit.toString(); editFeatures = p.features.joinToString(", ") }; isEditing = !isEditing }) {
                            Icon(if (isEditing) Icons.Default.Close else Icons.Default.Edit, null, tint = PMGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        when (val s = planState) {
            is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
            is PlatformViewModel.UiState.Error   -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(s.message, color = PMRed) }
            is PlatformViewModel.UiState.Success -> {
                val plan = s.data
                val isSuspended = plan.status == "suspended"
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Plan details card
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Plan Details", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            if (isEditing) {
                                OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                                OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") }, maxLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(value = editPrice, onValueChange = { editPrice = it }, label = { Text("Price ($)") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                                    OutlinedTextField(value = editScanLimit, onValueChange = { editScanLimit = it }, label = { Text("Scans/day") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                                }
                                OutlinedTextField(value = editFeatures, onValueChange = { editFeatures = it }, label = { Text("Features (comma-separated)") }, maxLines = 4, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                                val isSaving = planAction is PlatformViewModel.UiState.Loading
                                Button(
                                    onClick = {
                                        viewModel.updatePlan(token, planId, PMUpdatePlanRequest(
                                            name = editName.trim().ifBlank { null },
                                            description = editDesc.trim().ifBlank { null },
                                            price = editPrice.toDoubleOrNull(),
                                            scanLimit = editScanLimit.toIntOrNull(),
                                            features = editFeatures.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        )) {}
                                    },
                                    enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = PMGreen)
                                ) { if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold) }
                            } else {
                                PlanDetailRow("Name", plan.name)
                                PlanDetailRow("Price", if (plan.price == 0.0) "Free" else "$${String.format("%.2f", plan.price)}/month")
                                PlanDetailRow("Scan Limit", if (plan.scanLimit == 0) "Unlimited" else "${plan.scanLimit}/day")
                                PlanDetailRow("Status", plan.status.replaceFirstChar { it.uppercase() })
                                PlanDetailRow("Users", "${plan.userCount ?: 0}")
                                if (!plan.description.isNullOrBlank()) PlanDetailRow("Description", plan.description)
                                if (plan.features.isNotEmpty()) {
                                    Text("Features", fontSize = 13.sp, color = PMMuted, fontWeight = FontWeight.Medium)
                                    plan.features.forEach { f ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, null, tint = PMGreen, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(f, fontSize = 13.sp, color = PMTxt)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Plan status action
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Plan Actions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            if (isSuspended) {
                                OutlinedButton(onClick = { confirmActivate = true }, modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = PMGreen)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = PMGreen, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Activate Plan", color = PMGreen, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                OutlinedButton(onClick = { confirmSuspend = true }, modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = PMRed)) {
                                    Icon(Icons.Default.Block, null, tint = PMRed, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Suspend Plan", color = PMRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            else -> {}
        }
    }
}

@Composable private fun PlanDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = PMMuted, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = PMTxt, fontWeight = FontWeight.SemiBold)
    }
}
