package com.example.weblinkscanner.ui.screens.admin

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.AuditLogEntry
import com.example.weblinkscanner.viewmodel.AdminViewModel

private val ALBlue   = Color(0xFF1D4ED8); private val ALBlueLt  = Color(0xFFDBEAFE)
private val ALGreen  = Color(0xFF16A34A); private val ALGreenBg = Color(0xFFDCFCE7)
private val ALAmber  = Color(0xFFD97706); private val ALAmberBg = Color(0xFFFEF3C7)
private val ALRed    = Color(0xFFDC2626); private val ALRedBg   = Color(0xFFFEE2E2)
private val ALTxt    = Color(0xFF0F172A)
private val ALMuted  = Color(0xFF64748B)
private val ALPageBg = Color(0xFFF1F5F9)
private val ALCardBg = Color.White

private fun auditActionColor(action: String): Pair<Color, Color> = when {
    action.contains("suspend", ignoreCase = true) ||
    action.contains("lock",    ignoreCase = true)    -> ALRed   to ALRedBg
    action.contains("reactivate", ignoreCase = true) ||
    action.contains("unlock",  ignoreCase = true) ||
    action.contains("create",  ignoreCase = true)    -> ALGreen to ALGreenBg
    action.contains("update",  ignoreCase = true) ||
    action.contains("assign",  ignoreCase = true)    -> ALAmber to ALAmberBg
    else                                              -> ALBlue  to ALBlueLt
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    token:     String,
    viewModel: AdminViewModel,
    onBack:    () -> Unit
) {
    val auditState = viewModel.auditLog.collectAsState()
    val context    = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadAuditLog(token) }

    Scaffold(
        containerColor = ALPageBg,
        topBar = {
            TopAppBar(
                title = { Text("Audit Log", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (auditState.value is AdminViewModel.UiState.Success) {
                        val entries = (auditState.value as AdminViewModel.UiState.Success).data
                        IconButton(onClick = {
                            val csv = buildString {
                                appendLine("Action,Target Type,Target Email,Details,Timestamp")
                                entries.forEach { e ->
                                    appendLine("${e.action},${e.targetType ?: ""},${e.targetEmail ?: ""},\"${e.details ?: ""}\",${e.createdAt}")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "Audit Log Export")
                            }
                            context.startActivity(Intent.createChooser(intent, "Export CSV"))
                        }) { Icon(Icons.Default.Share, null, tint = ALBlue) }
                    }
                    IconButton(onClick = { viewModel.loadAuditLog(token) }) {
                        Icon(Icons.Default.Refresh, null, tint = ALBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ALCardBg, titleContentColor = ALTxt)
            )
        }
    ) { padding ->
        when (val s = auditState.value) {
            is AdminViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ALBlue)
                }
            }
            is AdminViewModel.UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.message, color = ALRed)
                }
            }
            is AdminViewModel.UiState.Success -> {
                val entries = s.data
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EventNote, null, tint = ALMuted, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No audit entries yet", fontSize = 16.sp, color = ALTxt, fontWeight = FontWeight.Medium)
                            Text("Admin actions will appear here", fontSize = 13.sp, color = ALMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            Text("${entries.size} event${if (entries.size != 1) "s" else ""} logged",
                                fontSize = 13.sp, color = ALMuted,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        items(entries) { entry -> AuditEntryRow(entry) }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun AuditEntryRow(entry: AuditLogEntry) {
    val (actionColor, actionBg) = auditActionColor(entry.action)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = ALCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            // Action icon circle
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(actionBg),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    entry.action.contains("suspend",    ignoreCase = true) -> Icons.Default.Block
                    entry.action.contains("reactivate", ignoreCase = true) -> Icons.Default.CheckCircle
                    entry.action.contains("lock",       ignoreCase = true) -> Icons.Default.Lock
                    entry.action.contains("unlock",     ignoreCase = true) -> Icons.Default.LockOpen
                    entry.action.contains("create",     ignoreCase = true) -> Icons.Default.PersonAdd
                    entry.action.contains("update",     ignoreCase = true) -> Icons.Default.Edit
                    entry.action.contains("assign",     ignoreCase = true) -> Icons.Default.AssignmentInd
                    else                                                    -> Icons.Default.EventNote
                }
                Icon(icon, null, tint = actionColor, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.action, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ALTxt)
                if (!entry.targetEmail.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(entry.targetEmail, fontSize = 12.sp, color = ALMuted)
                }
                if (!entry.details.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(entry.details, fontSize = 12.sp, color = ALMuted)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.createdAt.take(16).replace("T", " "),
                    fontSize = 11.sp, color = ALMuted
                )
            }

            // Type badge
            entry.targetType?.let { type ->
                Surface(shape = RoundedCornerShape(6.dp), color = actionBg) {
                    Text(type.replaceFirstChar { it.uppercase() }, fontSize = 10.sp,
                        color = actionColor, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}
