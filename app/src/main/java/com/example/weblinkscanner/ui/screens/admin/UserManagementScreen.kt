package com.example.weblinkscanner.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.AdminUser
import com.example.weblinkscanner.data.models.CreateAdminUserRequest
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val AdminBlue   = Color(0xFF1D4ED8)
private val AdminBlueBg = Color(0xFFEFF6FF)
private val AdminBlueLt = Color(0xFFDBEAFE)
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val CardBg      = Color.White
private val PageBg      = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    token:     String,
    viewModel: AdminViewModel,
    onUserClick: (userId: String) -> Unit,
    onBack:    () -> Unit
) {
    val usersState   by viewModel.users.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var searchQuery      by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var snackMsg         by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadUsers(token) }

    LaunchedEffect(actionResult) {
        if (actionResult is AdminViewModel.UiState.Success) {
            snackMsg = (actionResult as AdminViewModel.UiState.Success).data
            snackbarHostState.showSnackbar(snackMsg)
            viewModel.clearActionResult()
            viewModel.loadUsers(token, searchQuery.ifBlank { null })
        } else if (actionResult is AdminViewModel.UiState.Error) {
            snackMsg = (actionResult as AdminViewModel.UiState.Error).message
            snackbarHostState.showSnackbar(snackMsg)
            viewModel.clearActionResult()
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            token     = token,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = { showCreateDialog = true },
                containerColor    = AdminBlue,
                contentColor      = Color.White,
                shape             = CircleShape
            ) {
                Icon(Icons.Default.PersonAdd, "Add User")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Search Bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.loadUsers(token, it.ifBlank { null })
                },
                placeholder   = { Text("Search by name or email…", color = TextMuted) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.loadUsers(token)
                        }) { Icon(Icons.Default.Clear, null, tint = TextMuted) }
                    }
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AdminBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = AdminBlue
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── User List ─────────────────────────────────────────────────────
            when (val state = usersState) {
                is AdminViewModel.UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminBlue)
                    }
                }
                is AdminViewModel.UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFDC2626), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, color = Color(0xFFDC2626), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadUsers(token) },
                                colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is AdminViewModel.UiState.Success -> {
                    val users = state.data
                    if (users.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Group, null, tint = TextMuted, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "No users match \"$searchQuery\""
                                    else "No users found",
                                    color = TextMuted, fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Text("${users.size} user${if (users.size != 1) "s" else ""}", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(users, key = { it.id }) { user ->
                                UserRow(
                                    user        = user,
                                    onClick     = { onUserClick(user.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun UserRow(user: AdminUser, onClick: () -> Unit) {
    val initials = (user.name ?: user.email ?: "?")
        .trim().split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercaseChar().toString() }

    val (statusColor, statusBg) = when (user.accountStatus?.lowercase()) {
        "suspended" -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
        "locked"    -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        else        -> Color(0xFF16A34A) to Color(0xFFDCFCE7)
    }
    val statusLabel = when (user.accountStatus?.lowercase()) {
        "suspended" -> "Suspended"
        "locked"    -> "Locked"
        else        -> "Active"
    }
    val roleColor = when (user.role?.lowercase()) {
        "admin"            -> Color(0xFF7C3AED)
        "platform_manager" -> Color(0xFF0369A1)
        else               -> Color(0xFF374151)
    }
    val roleLabel = when (user.role?.lowercase()) {
        "admin"            -> "Admin"
        "platform_manager" -> "PM"
        else               -> "User"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AdminBlueLt),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminBlue)
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name ?: "No Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    user.email ?: "", fontSize = 12.sp, color = TextMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Role badge
                    Surface(shape = RoundedCornerShape(4.dp), color = roleColor.copy(alpha = 0.1f)) {
                        Text(roleLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = roleColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    // Status badge
                    Surface(shape = RoundedCornerShape(4.dp), color = statusBg) {
                        Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    // Plan badge
                    if (!user.plan.isNullOrBlank() && user.role !in listOf("admin", "platform_manager")) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEDE9FE)) {
                            Text(user.plan.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    token:     String,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val roleOptions = listOf("User", "Admin", "Platform Manager")
    val planOptions = listOf("free", "standard", "premium")

    var name             by remember { mutableStateOf("") }
    var email            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var showPassword     by remember { mutableStateOf(false) }
    var selectedRole     by remember { mutableStateOf(roleOptions[0]) }
    var selectedPlan     by remember { mutableStateOf(planOptions[0]) }
    var roleExpanded     by remember { mutableStateOf(false) }
    var planExpanded     by remember { mutableStateOf(false) }
    var error            by remember { mutableStateOf("") }

    val actionResult by viewModel.actionResult.collectAsState()
    val isLoading = actionResult is AdminViewModel.UiState.Loading

    LaunchedEffect(actionResult) {
        if (actionResult is AdminViewModel.UiState.Success) onDismiss()
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(16.dp),
        title = { Text("Create New User", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Full Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = dialogFieldColors()
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = dialogFieldColors()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = dialogFieldColors()
                )
                // Role dropdown
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = selectedRole, onValueChange = {}, readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp),
                        colors = dialogFieldColors()
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        roleOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = { selectedRole = opt; roleExpanded = false })
                        }
                    }
                }
                // Plan dropdown
                ExposedDropdownMenuBox(expanded = planExpanded, onExpandedChange = { planExpanded = it }) {
                    OutlinedTextField(
                        value = selectedPlan.replaceFirstChar { it.uppercase() }, onValueChange = {}, readOnly = true,
                        label = { Text("Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp),
                        colors = dialogFieldColors()
                    )
                    ExposedDropdownMenu(expanded = planExpanded, onDismissRequest = { planExpanded = false }) {
                        planOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt.replaceFirstChar { it.uppercase() }) },
                                onClick = { selectedPlan = opt; planExpanded = false })
                        }
                    }
                }
                if (error.isNotEmpty()) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.length < 6) {
                        error = "Please fill all fields. Password must be at least 6 characters."
                        return@Button
                    }
                    error = ""
                    viewModel.createUser(
                        token   = token,
                        request = CreateAdminUserRequest(
                            name     = name.trim(),
                            email    = email.trim(),
                            password = password,
                            role     = selectedRole.lowercase().replace(" ", "_"),
                            plan     = selectedPlan
                        ),
                        onDone  = {}
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isLoading) onDismiss() }) { Text("Cancel", color = TextMuted) }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AdminBlue,
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedLabelColor    = AdminBlue,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AdminBlue
)
