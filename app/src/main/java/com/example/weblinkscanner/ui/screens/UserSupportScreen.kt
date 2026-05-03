package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.UserSupportRequest
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import kotlinx.coroutines.launch

private val Blue600   = Color(0xFF2563EB)
private val Blue50    = Color(0xFFEFF6FF)
private val Blue100   = Color(0xFFDBEAFE)
private val PageBgTop = Color(0xFFEFF6FF)
private val PageBgBot = Color(0xFFF8FAFC)
private val CardBg    = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val GreenOk   = Color(0xFF16A34A); private val GreenBg = Color(0xFFF0FDF4)
private val AmberCol  = Color(0xFFD97706); private val AmberBg = Color(0xFFFEF3C7)
private val RedCol    = Color(0xFFDC2626); private val RedBg   = Color(0xFFFEE2E2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSupportScreen(
    repository: WeblinkScannerRepository,
    userId:     String,
    userEmail:  String,
    onBack:     () -> Unit
) {
    val scope = rememberCoroutineScope()
    var requests   by remember { mutableStateOf<List<UserSupportRequest>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var showNewDlg by remember { mutableStateOf(false) }
    var selectedReq by remember { mutableStateOf<UserSupportRequest?>(null) }

    fun load() {
        isLoading = true; errorMsg = null
        scope.launch {
            when (val r = repository.getMySupportRequests(userId)) {
                is Result.Success -> { requests = r.data.requests; isLoading = false }
                is Result.Error   -> { errorMsg = r.message; isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    // New Request Dialog
    if (showNewDlg) {
        NewSupportDialog(
            onDismiss = { showNewDlg = false },
            onSubmit  = { subject, message ->
                showNewDlg = false
                scope.launch {
                    repository.submitSupportRequest(userId, userEmail, subject, message)
                    load()
                }
            }
        )
    }

    // Detail / Reply Dialog
    selectedReq?.let { req ->
        SupportDetailDialog(
            request   = req,
            userEmail = userEmail,
            onDismiss = { selectedReq = null },
            onReply   = { message ->
                scope.launch {
                    repository.userReplySupport(req.id, message, userEmail)
                    selectedReq = null
                    load()
                }
            }
        )
    }

    Scaffold(
        containerColor = PageBgBot,
        topBar = {
            TopAppBar(
                title = { Text("My Reports", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, null, tint = Blue600) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg, titleContentColor = TextPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDlg = true },
                containerColor = Blue600
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Blue600)
                errorMsg != null -> Text(errorMsg!!, color = RedCol, modifier = Modifier.align(Alignment.Center).padding(24.dp), textAlign = TextAlign.Center)
                requests.isEmpty() -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SupportAgent, null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No reports yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Tap + to send a report to the platform team", fontSize = 13.sp, color = TextMuted)
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    items(requests) { req ->
                        SupportRequestCard(req = req, onClick = { selectedReq = req })
                    }
                }
            }
        }
    }
}

// Support Request Card
@Composable
private fun SupportRequestCard(req: UserSupportRequest, onClick: () -> Unit) {
    val (statusColor, statusBg, statusLabel) = when (req.status) {
        "open"        -> Triple(Blue600,  Blue50,   "Open")
        "in_progress" -> Triple(AmberCol, AmberBg,  "In Progress")
        "resolved"    -> Triple(GreenOk,  GreenBg,  "Resolved")
        else          -> Triple(TextMuted, Color(0xFFF1F5F9), req.status)
    }
    val hasNewReply = req.replies.any { it.senderType == "platform_manager" }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).background(Blue50, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.SupportAgent, null, tint = Blue600, modifier = Modifier.size(22.dp)) }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(req.subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (hasNewReply) {
                        Surface(shape = RoundedCornerShape(4.dp), color = GreenBg) {
                            Text("Reply", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenOk,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(req.message, fontSize = 12.sp, color = TextMuted, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                        Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Text("${req.replies.size} repl${if (req.replies.size != 1) "ies" else "y"}",
                        fontSize = 11.sp, color = TextMuted)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

// New Support Request Dialog
@Composable
private fun NewSupportDialog(onDismiss: () -> Unit, onSubmit: (subject: String, message: String) -> Unit) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var subjectError by remember { mutableStateOf("") }
    var messageError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send a Report", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Describe your issue and the platform team will respond.", fontSize = 13.sp, color = TextMuted)
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it; subjectError = "" },
                    label = { Text("Subject") },
                    isError = subjectError.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                if (subjectError.isNotEmpty()) Text(subjectError, color = RedCol, fontSize = 11.sp)
                OutlinedTextField(
                    value = message, onValueChange = { message = it; messageError = "" },
                    label = { Text("Message") },
                    isError = messageError.isNotEmpty(),
                    minLines = 4, maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                if (messageError.isNotEmpty()) Text(messageError, color = RedCol, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var valid = true
                    if (subject.isBlank()) { subjectError = "Subject is required"; valid = false }
                    if (message.isBlank()) { messageError = "Message is required"; valid = false }
                    if (valid) onSubmit(subject.trim(), message.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) { Text("Submit", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// Support Detail / Reply Dialog
@Composable
private fun SupportDetailDialog(
    request:   UserSupportRequest,
    userEmail: String,
    onDismiss: () -> Unit,
    onReply:   (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(request.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val (statusColor, statusBg, statusLabel) = when (request.status) {
                    "open"        -> Triple(Blue600,  Blue50,  "Open")
                    "in_progress" -> Triple(AmberCol, AmberBg, "In Progress")
                    "resolved"    -> Triple(GreenOk,  GreenBg, "Resolved")
                    else          -> Triple(TextMuted, Color(0xFFF1F5F9), request.status)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Original message
                Surface(shape = RoundedCornerShape(10.dp), color = Blue50) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("You", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Blue600)
                        Spacer(Modifier.height(4.dp))
                        Text(request.message, fontSize = 13.sp, color = TextPrimary)
                    }
                }

                // Replies
                request.replies.forEach { reply ->
                    val isUser = reply.senderType == "user"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUser) Blue50 else GreenBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                if (isUser) "You" else "Platform Team",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (isUser) Blue600 else GreenOk
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(reply.message, fontSize = 13.sp, color = TextPrimary)
                            reply.createdAt?.take(16)?.replace("T", " ")?.let {
                                Text(it, fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }

                if (request.status != "resolved") {
                    HorizontalDivider()
                    Text("Reply", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    OutlinedTextField(
                        value = replyText, onValueChange = { replyText = it },
                        placeholder = { Text("Type your reply...", color = TextMuted) },
                        minLines = 2, maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (request.status != "resolved") {
                Button(
                    onClick = { if (replyText.isNotBlank()) onReply(replyText.trim()) },
                    colors  = ButtonDefaults.buttonColors(containerColor = Blue600),
                    enabled = replyText.isNotBlank()
                ) { Text("Send Reply", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextMuted) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
