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
import com.example.weblinkscanner.data.models.CreateProfileRequest
import com.example.weblinkscanner.data.models.UserProfile
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val PBlue    = Color(0xFF1D4ED8)
private val PBlueBg  = Color(0xFFEFF6FF)
private val PBlueLt  = Color(0xFFDBEAFE)
private val PGreen   = Color(0xFF16A34A)
private val PGreenBg = Color(0xFFDCFCE7)
private val PAmber   = Color(0xFFD97706)
private val PAmberBg = Color(0xFFFEF3C7)
private val PTxt     = Color(0xFF0F172A)
private val PMuted   = Color(0xFF64748B)
private val PPageBg  = Color(0xFFF1F5F9)
private val PCardBg  = Color.White

val ALL_PERMISSIONS = listOf(
    "scan_url"        to "Scan URLs",
    "scan_camera"     to "Scan via Camera",
    "scan_qr"         to "Scan QR Codes",
    "sandbox_analyse" to "Sandbox Analysis",
    "view_history"    to "View Scan History",
    "saved_links"     to "Save Links",
    "export_data"     to "Export Data"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfilesScreen(
    token:          String,
    viewModel:      AdminViewModel,
    onProfileClick: (String) -> Unit,
    onBack:         () -> Unit
) {
    val profilesState by viewModel.profiles.collectAsState()
    val actionResult  by viewModel.profileActionResult.collectAsState()

    var searchQuery      by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadProfiles(token) }

    LaunchedEffect(actionResult) {
        if (actionResult is AdminViewModel.UiState.Success) {
            snackbarHostState.showSnackbar((actionResult as AdminViewModel.UiState.Success<String>).data)
            viewModel.clearProfileActionResult()
            viewModel.loadProfiles(token)
        } else if (actionResult is AdminViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((actionResult as AdminViewModel.UiState.Error).message)
            viewModel.clearProfileActionResult()
        }
    }

    Scaffold(
        containerColor   = PPageBg,
        snackbarHost     = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Profiles", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = PCardBg,
                    titleContentColor = PTxt
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = { showCreateDialog = true },
                containerColor    = PBlue,
                contentColor      = Color.White,
                shape             = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Profile")
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

            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.loadProfiles(token, it.ifBlank { null })
                },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Search profiles…", color = PMuted) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = PMuted) },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.loadProfiles(token)
                        }) { Icon(Icons.Default.Clear, null, tint = PMuted) }
                    }
                },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor   = PCardBg,
                    unfocusedContainerColor = PCardBg
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val s = profilesState) {
                is AdminViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PBlue)
                    }
                }
                is AdminViewModel.UiState.Success -> {
                    if (s.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No profiles found", color = PMuted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(s.data) { profile ->
                                ProfileRow(profile = profile, onClick = { onProfileClick(profile.id) })
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
                is AdminViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = Color.Red, fontSize = 14.sp)
                    }
                }
                else -> {}
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate  = { req ->
                viewModel.createProfile(token, req) { showCreateDialog = false }
            }
        )
    }
}

@Composable
private fun ProfileRow(profile: UserProfile, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PCardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PBlueLt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ManageAccounts, null, tint = PBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(profile.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PTxt)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Status badge
                        val (badgeColor, badgeBg) = if (profile.status == "active")
                            PGreen to PGreenBg else PAmber to PAmberBg
                        Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                            Text(
                                profile.status.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            "${profile.permissions.size} permission${if (profile.permissions.size != 1) "s" else ""}",
                            fontSize = 12.sp, color = PMuted
                        )
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = PMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate:  (CreateProfileRequest) -> Unit
) {
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var permissions by remember { mutableStateOf(setOf<String>()) }
    var nameError   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = PCardBg,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text("Create Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PTxt)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    label         = { Text("Profile Name *") },
                    isError       = nameError,
                    supportingText = if (nameError) {{ Text("Name is required") }} else null,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 2,
                    shape         = RoundedCornerShape(10.dp)
                )

                Text("Permissions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PMuted)

                ALL_PERMISSIONS.forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 14.sp, color = PTxt)
                        Switch(
                            checked         = key in permissions,
                            onCheckedChange = { checked ->
                                permissions = if (checked) permissions + key else permissions - key
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PBlue)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    onCreate(CreateProfileRequest(
                        name        = name.trim(),
                        description = description.trim(),
                        permissions = permissions.toList()
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = PBlue),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PMuted) }
        }
    )
}
