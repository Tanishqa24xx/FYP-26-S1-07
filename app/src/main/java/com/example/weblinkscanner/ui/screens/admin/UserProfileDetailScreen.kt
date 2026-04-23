package com.example.weblinkscanner.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.weblinkscanner.data.models.AdminProfileUpdateRequest
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val DBlue    = Color(0xFF1D4ED8)
private val DBlueLt  = Color(0xFFDBEAFE)
private val DGreen   = Color(0xFF16A34A)
private val DGreenBg = Color(0xFFDCFCE7)
private val DAmber   = Color(0xFFD97706)
private val DAmberBg = Color(0xFFFEF3C7)
private val DRed     = Color(0xFFDC2626)
private val DRedBg   = Color(0xFFFEE2E2)
private val DTxt     = Color(0xFF0F172A)
private val DMuted   = Color(0xFF64748B)
private val DPageBg  = Color(0xFFF1F5F9)
private val DCardBg  = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDetailScreen(
    profileId: String,
    token:     String,
    viewModel: AdminViewModel,
    onBack:    () -> Unit
) {
    val profileState  by viewModel.selectedProfile.collectAsState()
    val actionResult  by viewModel.profileActionResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEditing        by remember { mutableStateOf(false) }
    var editName         by remember { mutableStateOf("") }
    var editDescription  by remember { mutableStateOf("") }
    var editPermissions  by remember { mutableStateOf(setOf<String>()) }
    var confirmSuspend   by remember { mutableStateOf(false) }
    var confirmReactivate by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) { viewModel.loadProfile(token, profileId) }

    LaunchedEffect(profileState) {
        if (profileState is AdminViewModel.UiState.Success) {
            val p = (profileState as AdminViewModel.UiState.Success).data
            if (editName.isBlank()) editName = p.name
            if (editDescription.isBlank()) editDescription = p.description ?: ""
            if (editPermissions.isEmpty() && p.permissions.isNotEmpty()) {
                editPermissions = p.permissions.toSet()
            }
        }
    }

    LaunchedEffect(actionResult) {
        when (actionResult) {
            is AdminViewModel.UiState.Success -> {
                snackbarHostState.showSnackbar(
                    (actionResult as AdminViewModel.UiState.Success<String>).data
                )
                viewModel.clearProfileActionResult()
                isEditing = false
            }
            is AdminViewModel.UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (actionResult as AdminViewModel.UiState.Error).message
                )
                viewModel.clearProfileActionResult()
            }
            else -> {}
        }
    }

    // Confirm dialogs
    if (confirmSuspend) {
        AlertDialog(
            onDismissRequest = { confirmSuspend = false },
            shape = RoundedCornerShape(16.dp),
            icon  = { Icon(Icons.Default.Block, null, tint = DRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Suspend Profile?", fontWeight = FontWeight.Bold) },
            text  = { Text("This profile will be suspended. No new users can be assigned to it.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.suspendProfile(token, profileId); confirmSuspend = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = DRed),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Suspend", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSuspend = false }) { Text("Cancel", color = DMuted) }
            }
        )
    }

    if (confirmReactivate) {
        AlertDialog(
            onDismissRequest = { confirmReactivate = false },
            shape = RoundedCornerShape(16.dp),
            icon  = { Icon(Icons.Default.CheckCircle, null, tint = DGreen, modifier = Modifier.size(36.dp)) },
            title = { Text("Reactivate Profile?", fontWeight = FontWeight.Bold) },
            text  = { Text("This profile will be reactivated and can be assigned to users again.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.reactivateProfile(token, profileId); confirmReactivate = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = DGreen),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Reactivate", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReactivate = false }) { Text("Cancel", color = DMuted) }
            }
        )
    }

    Scaffold(
        containerColor = DPageBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile Detail", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (profileState is AdminViewModel.UiState.Success) {
                        IconButton(onClick = {
                            if (isEditing) {
                                // Reset edits
                                val p = (profileState as AdminViewModel.UiState.Success).data
                                editName        = p.name
                                editDescription = p.description ?: ""
                                editPermissions = p.permissions.toSet()
                            }
                            isEditing = !isEditing
                        }) {
                            Icon(
                                if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                null, tint = DBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = DCardBg,
                    titleContentColor = DTxt
                )
            )
        }
    ) { padding ->
        when (val state = profileState) {
            is AdminViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DBlue)
                }
            }
            is AdminViewModel.UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = DRed)
                }
            }
            is AdminViewModel.UiState.Success -> {
                val profile = state.data
                val isSuspended = profile.status == "suspended"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Header Card ───────────────────────────────────────────
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = DCardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(DBlueLt),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ManageAccounts, null, tint = DBlue, modifier = Modifier.size(34.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(profile.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DTxt)
                            if (!profile.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(profile.description, fontSize = 13.sp, color = DMuted)
                            }
                            Spacer(Modifier.height(8.dp))
                            val (badgeColor, badgeBg) = if (!isSuspended) DGreen to DGreenBg else DAmber to DAmberBg
                            Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                                Text(
                                    if (!isSuspended) "Active" else "Suspended",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${profile.permissions.size} permission${if (profile.permissions.size != 1) "s" else ""}  •  " +
                                "${profile.assignedUsers?.size ?: 0} assigned user${if ((profile.assignedUsers?.size ?: 0) != 1) "s" else ""}",
                                fontSize = 12.sp, color = DMuted
                            )
                        }
                    }

                    // ── Permissions Card ──────────────────────────────────────
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = DCardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Permissions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DMuted)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))

                            if (isEditing) {
                                // Name field
                                OutlinedTextField(
                                    value = editName, onValueChange = { editName = it },
                                    label = { Text("Profile Name") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editDescription, onValueChange = { editDescription = it },
                                    label = { Text("Description") }, maxLines = 2,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Toggle Permissions", fontSize = 13.sp, color = DMuted, fontWeight = FontWeight.Medium)
                            }

                            ALL_PERMISSIONS.forEach { (key, label) ->
                                if (isEditing) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(label, fontSize = 14.sp, color = DTxt)
                                        Switch(
                                            checked = key in editPermissions,
                                            onCheckedChange = { checked ->
                                                editPermissions = if (checked) editPermissions + key else editPermissions - key
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = DBlue
                                            )
                                        )
                                    }
                                } else {
                                    val hasPermission = key in profile.permissions
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            null,
                                            tint = if (hasPermission) DGreen else Color(0xFFCBD5E1),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            label,
                                            fontSize = 14.sp,
                                            color = if (hasPermission) DTxt else DMuted
                                        )
                                    }
                                }
                            }

                            if (isEditing) {
                                Spacer(Modifier.height(8.dp))
                                val isSaving = actionResult is AdminViewModel.UiState.Loading
                                Button(
                                    onClick = {
                                        viewModel.updateProfile(
                                            token     = token,
                                            profileId = profileId,
                                            request   = AdminProfileUpdateRequest(
                                                name        = editName.trim().ifBlank { null },
                                                description = editDescription.trim().ifBlank { null },
                                                permissions = editPermissions.toList()
                                            ),
                                            onDone = {}
                                        )
                                    },
                                    enabled  = !isSaving,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape    = RoundedCornerShape(10.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = DBlue)
                                ) {
                                    if (isSaving) CircularProgressIndicator(
                                        color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                                    )
                                    else Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ── Profile Actions Card ──────────────────────────────────
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = DCardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Profile Actions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DMuted)
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            if (isSuspended) {
                                ProfileActionButton("Reactivate Profile", Icons.Default.CheckCircle, DGreen) {
                                    confirmReactivate = true
                                }
                            } else {
                                ProfileActionButton("Suspend Profile", Icons.Default.Block, DRed) {
                                    confirmSuspend = true
                                }
                            }
                        }
                    }

                    // ── Assigned Users Card ───────────────────────────────────
                    val assignedUsers = profile.assignedUsers ?: emptyList()
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = DCardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Assigned Users (${assignedUsers.size})",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DMuted
                            )
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                            if (assignedUsers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No users assigned to this profile", fontSize = 13.sp, color = DMuted)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    assignedUsers.forEach { user ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val initials = (user.name ?: user.email ?: "?")
                                                .trim().split(" ").filter { it.isNotBlank() }
                                                .take(2).joinToString("") { it.first().uppercaseChar().toString() }
                                            Box(
                                                modifier = Modifier.size(36.dp).clip(CircleShape).background(DBlueLt),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(initials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DBlue)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(user.name ?: "No Name", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DTxt)
                                                Text(user.email ?: "", fontSize = 12.sp, color = DMuted)
                                            }
                                            Spacer(Modifier.weight(1f))
                                            user.role?.let { role ->
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEFF6FF)) {
                                                    Text(
                                                        role.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                        fontSize = 11.sp, color = DBlue, fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (user != assignedUsers.last()) {
                                            HorizontalDivider(color = Color(0xFFE2E8F0))
                                        }
                                    }
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

@Composable
private fun ProfileActionButton(
    label: String,
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape    = RoundedCornerShape(10.dp),
        border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
