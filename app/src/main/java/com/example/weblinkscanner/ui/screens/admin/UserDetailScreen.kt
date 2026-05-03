package com.example.weblinkscanner.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.weblinkscanner.data.models.AssignProfileRequest
import com.example.weblinkscanner.data.models.UpdateAdminUserRequest
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val AdminBlue   = Color(0xFF1D4ED8)
private val AdminBlueLt = Color(0xFFDBEAFE)
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val CardBg      = Color.White
private val PageBg      = Color(0xFFF1F5F9)
private val DangerRed   = Color(0xFFDC2626)
private val WarningAmb  = Color(0xFFD97706)
private val SuccessGrn  = Color(0xFF16A34A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId:    String,
    token:     String,
    viewModel: AdminViewModel,
    onBack:    () -> Unit
) {
    val userState     by viewModel.selectedUser.collectAsState()
    val actionResult  by viewModel.actionResult.collectAsState()
    val profilesState by viewModel.profiles.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Edit state
    var editName     by remember { mutableStateOf("") }
    var editRole     by remember { mutableStateOf("") }
    var editPlan     by remember { mutableStateOf("") }
    var roleExpanded by remember { mutableStateOf(false) }
    var planExpanded by remember { mutableStateOf(false) }
    var isEditing    by remember { mutableStateOf(false) }

    // Profile assignment state
    var profileExpanded      by remember { mutableStateOf(false) }
    var selectedProfileId    by remember { mutableStateOf<String?>(null) }
    var selectedProfileName  by remember { mutableStateOf("Select a profile") }
    var confirmRemoveProfile by remember { mutableStateOf(false) }

    val roleOptions = listOf("user", "admin", "platform_manager")
    val planOptions = listOf("free", "standard", "premium")

    var confirmAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        viewModel.loadUser(token, userId)
        viewModel.loadProfiles(token)
    }

    LaunchedEffect(userState) {
        if (userState is AdminViewModel.UiState.Success) {
            val u = (userState as AdminViewModel.UiState.Success).data
            if (editName.isBlank()) editName = u.name ?: ""
            if (editRole.isBlank()) editRole = u.role ?: "user"
            if (editPlan.isBlank()) editPlan = u.plan ?: "free"
        }
    }

    LaunchedEffect(actionResult) {
        if (actionResult is AdminViewModel.UiState.Success) {
            val msg = (actionResult as AdminViewModel.UiState.Success).data
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionResult()
            isEditing = false
        } else if (actionResult is AdminViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((actionResult as AdminViewModel.UiState.Error).message)
            viewModel.clearActionResult()
        }
    }

    // Confirm dialog
    if (confirmAction != null) {
        val isDanger = confirmAction in listOf("suspend", "lock", "reject_account")
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            shape = RoundedCornerShape(16.dp),
            icon = {
                val icon = when (confirmAction) {
                    "suspend"        -> Icons.Default.Block
                    "reactivate"     -> Icons.Default.CheckCircle
                    "lock"           -> Icons.Default.Lock
                    "unlock"         -> Icons.Default.LockOpen
                    "approve"        -> Icons.Default.CheckCircle
                    "reject_account" -> Icons.Default.Cancel
                    else             -> Icons.Default.Warning
                }
                Icon(icon, null, tint = if (isDanger) DangerRed else SuccessGrn,
                    modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    when (confirmAction) {
                        "suspend"        -> "Suspend User?"
                        "reactivate"     -> "Reactivate User?"
                        "lock"           -> "Lock Account?"
                        "unlock"         -> "Unlock Account?"
                        "approve"        -> "Approve Account?"
                        "reject_account" -> "Reject Account?"
                        else             -> "Confirm Action"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    when (confirmAction) {
                        "suspend"        -> "This user will no longer be able to log in."
                        "reactivate"     -> "This user will regain access to their account."
                        "lock"           -> "The account will be locked. The user cannot log in."
                        "unlock"         -> "Failed login count will be reset and access restored."
                        "approve"        -> "This user will be able to log in immediately."
                        "reject_account" -> "This user's registration will be rejected. They cannot log in."
                        else             -> ""
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (confirmAction) {
                            "suspend"        -> viewModel.suspendUser(token, userId)
                            "reactivate"     -> viewModel.reactivateUser(token, userId)
                            "lock"           -> viewModel.lockUser(token, userId)
                            "unlock"         -> viewModel.unlockUser(token, userId)
                            "approve"        -> viewModel.approveUser(token, userId)
                            "reject_account" -> viewModel.rejectUser(token, userId)
                        }
                        confirmAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDanger) DangerRed else SuccessGrn
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    if (confirmRemoveProfile) {
        AlertDialog(
            onDismissRequest = { confirmRemoveProfile = false },
            shape = RoundedCornerShape(16.dp),
            icon  = { Icon(Icons.Default.PersonRemove, null, tint = DangerRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Remove Profile?", fontWeight = FontWeight.Bold) },
            text  = { Text("The profile will be unassigned from this user.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeProfile(token, userId); confirmRemoveProfile = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Remove", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveProfile = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Detail", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (userState is AdminViewModel.UiState.Success) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                null, tint = AdminBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        when (val state = userState) {
            is AdminViewModel.UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminBlue)
                }
            }
            is AdminViewModel.UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = DangerRed)
                }
            }
            is AdminViewModel.UiState.Success -> {
                val user = state.data
                val accountStatus = user.accountStatus ?: "active"
                val (statusColor, statusBg) = when (accountStatus.lowercase()) {
                    "suspended" -> DangerRed to Color(0xFFFEE2E2)
                    "locked"    -> WarningAmb to Color(0xFFFEF3C7)
                    else        -> SuccessGrn to Color(0xFFDCFCE7)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar & Status
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val initials = (user.name ?: user.email ?: "?")
                                .trim().split(" ").filter { it.isNotBlank() }
                                .take(2).joinToString("") { it.first().uppercaseChar().toString() }
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape).background(AdminBlueLt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AdminBlue)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(user.name ?: "No Name", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(user.email ?: "", fontSize = 13.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                                Text(
                                    accountStatus.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            if ((user.failedLoginCount ?: 0) > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${user.failedLoginCount} failed login attempt(s)", fontSize = 11.sp, color = WarningAmb)
                            }
                        }
                    }

                    //  Edit / View Fields
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Account Details", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            if (isEditing) {
                                OutlinedTextField(
                                    value = editName, onValueChange = { editName = it },
                                    label = { Text("Full Name") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                    colors = detailFieldColors()
                                )
                                // Role dropdown
                                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                                    OutlinedTextField(
                                        value = editRole.replace("_", " ").replaceFirstChar { it.uppercase() },
                                        onValueChange = {}, readOnly = true,
                                        label = { Text("Role") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        shape = RoundedCornerShape(10.dp), colors = detailFieldColors()
                                    )
                                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                        roleOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                                onClick = { editRole = opt; roleExpanded = false }
                                            )
                                        }
                                    }
                                }
                                // Plan dropdown
                                ExposedDropdownMenuBox(expanded = planExpanded, onExpandedChange = { planExpanded = it }) {
                                    OutlinedTextField(
                                        value = editPlan.replaceFirstChar { it.uppercase() },
                                        onValueChange = {}, readOnly = true,
                                        label = { Text("Plan") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        shape = RoundedCornerShape(10.dp), colors = detailFieldColors()
                                    )
                                    ExposedDropdownMenu(expanded = planExpanded, onDismissRequest = { planExpanded = false }) {
                                        planOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.replaceFirstChar { it.uppercase() }) },
                                                onClick = { editPlan = opt; planExpanded = false }
                                            )
                                        }
                                    }
                                }
                                val isSaving = actionResult is AdminViewModel.UiState.Loading
                                Button(
                                    onClick = {
                                        viewModel.updateUser(
                                            token   = token,
                                            userId  = userId,
                                            request = UpdateAdminUserRequest(
                                                name = editName.trim().ifBlank { null },
                                                role = editRole.ifBlank { null },
                                                plan = editPlan.ifBlank { null }
                                            ),
                                            onDone  = {}
                                        )
                                    },
                                    enabled  = !isSaving,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape    = RoundedCornerShape(10.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                                ) {
                                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                DetailRow("Name",   user.name ?: "—")
                                DetailRow("Email",  user.email ?: "—")
                                DetailRow("Role",   (user.role ?: "user").replace("_", " ").replaceFirstChar { it.uppercase() })
                                DetailRow("Plan",   (user.plan ?: "free").replaceFirstChar { it.uppercase() })
                                DetailRow("Status", (user.status ?: "approved").replaceFirstChar { it.uppercase() })
                            }
                        }
                    }

                    // Pending Approval Banner
                    val registrationStatus = user.status ?: "approved"
                    if (registrationStatus == "pending") {
                        Card(
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassEmpty, null, tint = WarningAmb, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pending Approval", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WarningAmb)
                                }
                                Text("This account is awaiting your approval before the user can log in.",
                                    fontSize = 13.sp, color = TextMuted)
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick  = { confirmAction = "approve" },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape    = RoundedCornerShape(10.dp),
                                        colors   = ButtonDefaults.buttonColors(containerColor = SuccessGrn)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Approve", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick  = { confirmAction = "reject_account" },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape    = RoundedCornerShape(10.dp),
                                        border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                    ) {
                                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Account Actions
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Account Actions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            if (accountStatus == "suspended") {
                                ActionButton("Reactivate Account", Icons.Default.CheckCircle, SuccessGrn) { confirmAction = "reactivate" }
                            } else {
                                ActionButton("Suspend Account", Icons.Default.Block, DangerRed) { confirmAction = "suspend" }
                            }

                            if (accountStatus == "locked") {
                                ActionButton("Unlock Account", Icons.Default.LockOpen, SuccessGrn) { confirmAction = "unlock" }
                            } else {
                                ActionButton("Lock Account", Icons.Default.Lock, WarningAmb) { confirmAction = "lock" }
                            }
                        }
                    }

                    // Profile Assignment
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("User Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            // Show currently assigned profile
                            val currentProfileId   = user.profileId
                            val currentProfileName = user.profileName

                            if (currentProfileId != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Assigned Profile", fontSize = 12.sp, color = TextMuted)
                                        Text(
                                            currentProfileName ?: currentProfileId,
                                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                                        )
                                    }
                                    TextButton(onClick = { confirmRemoveProfile = true }) {
                                        Text("Remove", color = DangerRed, fontSize = 13.sp)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                            }

                            Text(
                                if (currentProfileId != null) "Change Profile" else "Assign a Profile",
                                fontSize = 13.sp, color = TextMuted
                            )

                            // Profile dropdown
                            val availableProfiles = when (val ps = profilesState) {
                                is AdminViewModel.UiState.Success -> ps.data.filter { it.status == "active" }
                                else -> emptyList()
                            }

                            ExposedDropdownMenuBox(
                                expanded = profileExpanded,
                                onExpandedChange = { profileExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value         = selectedProfileName,
                                    onValueChange = {},
                                    readOnly      = true,
                                    label         = { Text("Profile") },
                                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileExpanded) },
                                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                                    shape         = RoundedCornerShape(10.dp),
                                    colors        = detailFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded        = profileExpanded,
                                    onDismissRequest = { profileExpanded = false }
                                ) {
                                    if (availableProfiles.isEmpty()) {
                                        DropdownMenuItem(
                                            text    = { Text("No active profiles available", color = TextMuted) },
                                            onClick = { profileExpanded = false }
                                        )
                                    } else {
                                        availableProfiles.forEach { profile ->
                                            DropdownMenuItem(
                                                text = { Text(profile.name) },
                                                onClick = {
                                                    selectedProfileId   = profile.id
                                                    selectedProfileName = profile.name
                                                    profileExpanded     = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            val isSaving = actionResult is AdminViewModel.UiState.Loading
                            Button(
                                onClick  = {
                                    selectedProfileId?.let { pid ->
                                        viewModel.assignProfile(token, userId, pid)
                                    }
                                },
                                enabled  = selectedProfileId != null && !isSaving,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                            ) {
                                if (isSaving) CircularProgressIndicator(
                                    color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                                )
                                else Text(
                                    if (currentProfileId != null) "Change Profile" else "Assign Profile",
                                    color = Color.White, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape    = RoundedCornerShape(10.dp),
        border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun detailFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AdminBlue,
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedLabelColor    = AdminBlue,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AdminBlue
)
