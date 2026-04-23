package com.example.weblinkscanner.ui.screens.platform

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
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenLt = Color(0xFFD1FAE5); private val PMGreenBg = Color(0xFFECFDF5)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMTeal     = Color(0xFF0891B2); private val PMTealBg  = Color(0xFFE0F2FE)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMSupportDetailScreen(
    requestId: String,
    token:     String,
    viewModel: PlatformViewModel,
    onBack:    () -> Unit
) {
    val supportState  by viewModel.selectedSupport.collectAsState()
    val supportAction by viewModel.supportAction.collectAsState()
    val snackbar      = remember { SnackbarHostState() }
    var replyText     by remember { mutableStateOf("") }
    var showStatusMenu by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) { viewModel.loadSupportRequest(token, requestId) }

    LaunchedEffect(supportAction) {
        when (supportAction) {
            is PlatformViewModel.UiState.Success -> { snackbar.showSnackbar((supportAction as PlatformViewModel.UiState.Success<String>).data); viewModel.clearSupportAction() }
            is PlatformViewModel.UiState.Error   -> { snackbar.showSnackbar((supportAction as PlatformViewModel.UiState.Error).message); viewModel.clearSupportAction() }
            else -> {}
        }
    }

    Scaffold(
        containerColor = PMPageBg,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Support Detail", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        }
    ) { padding ->
        when (val s = supportState) {
            is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
            is PlatformViewModel.UiState.Error   -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(s.message, color = PMRed) }
            is PlatformViewModel.UiState.Success -> {
                val req = s.data
                val (statusColor, statusBg) = when (req.status) {
                    "open"        -> PMTeal  to PMTealBg
                    "in_progress" -> PMAmber to PMAmberBg
                    "resolved"    -> PMGreen to PMGreenBg
                    else          -> PMMuted to Color(0xFFF1F5F9)
                }
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header card
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PMCardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f)) {
                                        Text(req.subject, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PMTxt)
                                        Text(req.userEmail ?: "Unknown", fontSize = 13.sp, color = PMMuted)
                                        Text(req.createdAt?.take(10) ?: "", fontSize = 11.sp, color = PMMuted)
                                    }
                                    Box {
                                        Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                                            TextButton(onClick = { showStatusMenu = true }) {
                                                Text(req.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                                Icon(Icons.Default.ArrowDropDown, null, tint = statusColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                            listOf("open", "in_progress", "resolved").forEach { status ->
                                                DropdownMenuItem(
                                                    text = { Text(status.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                                    onClick = { viewModel.updateSupportStatus(token, requestId, status); showStatusMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Text("Original Message", fontSize = 12.sp, color = PMMuted, fontWeight = FontWeight.Medium)
                                Text(req.message, fontSize = 14.sp, color = PMTxt)
                            }
                        }

                        // Reply thread
                        val replies = req.replies ?: emptyList()
                        if (replies.isNotEmpty()) {
                            Text("Conversation (${replies.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMMuted)
                            replies.forEach { reply ->
                                val isPM = reply.senderType == "platform_manager"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isPM) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!isPM) {
                                        Box(Modifier.size(32.dp).clip(CircleShape).background(PMTealBg), contentAlignment = Alignment.Center) {
                                            Text("U", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PMTeal)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Card(
                                        modifier = Modifier.widthIn(max = 280.dp),
                                        shape    = RoundedCornerShape(12.dp),
                                        colors   = CardDefaults.cardColors(containerColor = if (isPM) PMGreenLt else Color(0xFFF1F5F9))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(reply.message, fontSize = 13.sp, color = PMTxt)
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "${if (isPM) "Platform Manager" else "User"} · ${reply.createdAt?.take(10) ?: ""}",
                                                fontSize = 11.sp, color = PMMuted
                                            )
                                        }
                                    }
                                    if (isPM) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(Modifier.size(32.dp).clip(CircleShape).background(PMGreenBg), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.SupportAgent, null, tint = PMGreen, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reply box
                    if (req.status != "resolved") {
                        Surface(shadowElevation = 8.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = replyText, onValueChange = { replyText = it },
                                    placeholder = { Text("Type a reply…") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                val isSending = supportAction is PlatformViewModel.UiState.Loading
                                IconButton(
                                    onClick = {
                                        if (replyText.isNotBlank()) {
                                            viewModel.replyToSupport(token, requestId, replyText) { replyText = "" }
                                        }
                                    },
                                    enabled = replyText.isNotBlank() && !isSending,
                                    modifier = Modifier.size(48.dp).background(PMGreen, CircleShape)
                                ) {
                                    if (isSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Default.Send, null, tint = Color.White)
                                }
                            }
                        }
                    } else {
                        Surface(color = PMGreenBg) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.CheckCircle, null, tint = PMGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("This request is resolved", fontSize = 13.sp, color = PMGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
