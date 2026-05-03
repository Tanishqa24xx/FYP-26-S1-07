package com.example.weblinkscanner.ui.screens.platform

import androidx.compose.animation.AnimatedVisibility
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
import com.example.weblinkscanner.data.models.PMCreateFaqRequest
import com.example.weblinkscanner.data.models.PMFaqItem
import com.example.weblinkscanner.data.models.PMUpdateFaqRequest
import com.example.weblinkscanner.viewmodel.PlatformViewModel

private val PMGreen    = Color(0xFF059669); private val PMGreenBg = Color(0xFFECFDF5)
private val PMAmber    = Color(0xFFD97706); private val PMAmberBg = Color(0xFFFEF3C7)
private val PMRed      = Color(0xFFDC2626); private val PMRedBg   = Color(0xFFFEE2E2)
private val PMTeal     = Color(0xFF0891B2); private val PMTealBg  = Color(0xFFE0F2FE)
private val PMTxt      = Color(0xFF0F172A)
private val PMMuted    = Color(0xFF64748B)
private val PMPageBg   = Color(0xFFF0FDF4)
private val PMCardBg   = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMFaqScreen(
    token:     String,
    viewModel: PlatformViewModel,
    onBack:    () -> Unit
) {
    val faqState  by viewModel.faqs.collectAsState()
    val faqAction by viewModel.faqAction.collectAsState()
    val snackbar  = remember { SnackbarHostState() }

    var showCreate  by remember { mutableStateOf(false) }
    var editingFaq  by remember { mutableStateOf<PMFaqItem?>(null) }
    var deletingFaq by remember { mutableStateOf<PMFaqItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadFaqs(token) }

    LaunchedEffect(faqAction) {
        when (faqAction) {
            is PlatformViewModel.UiState.Success -> { snackbar.showSnackbar((faqAction as PlatformViewModel.UiState.Success<String>).data); viewModel.clearFaqAction() }
            is PlatformViewModel.UiState.Error   -> { snackbar.showSnackbar((faqAction as PlatformViewModel.UiState.Error).message); viewModel.clearFaqAction() }
            else -> {}
        }
    }

    // Delete confirm dialog
    deletingFaq?.let { faq ->
        AlertDialog(
            onDismissRequest = { deletingFaq = null },
            shape = RoundedCornerShape(16.dp),
            icon  = { Icon(Icons.Default.Delete, null, tint = PMRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete FAQ?", fontWeight = FontWeight.Bold) },
            text  = { Text("\"${faq.question}\" will be permanently deleted.") },
            confirmButton = { Button(onClick = { viewModel.deleteFaq(token, faq.id); deletingFaq = null },
                colors = ButtonDefaults.buttonColors(containerColor = PMRed), shape = RoundedCornerShape(10.dp)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deletingFaq = null }) { Text("Cancel") } }
        )
    }

    // Edit dialog
    editingFaq?.let { faq ->
        FaqEditDialog(
            faq       = faq,
            onDismiss = { editingFaq = null },
            onSave    = { req -> viewModel.updateFaq(token, faq.id, req) { editingFaq = null } }
        )
    }

    // Create dialog
    if (showCreate) {
        FaqCreateDialog(
            onDismiss = { showCreate = false },
            onCreate  = { req -> viewModel.createFaq(token, req) { showCreate = false } }
        )
    }

    Scaffold(
        containerColor = PMPageBg,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("FAQ Management", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.loadFaqs(token) }) { Icon(Icons.Default.Refresh, null, tint = PMGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PMCardBg, titleContentColor = PMTxt)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = PMGreen, contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        when (val s = faqState) {
            is PlatformViewModel.UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PMGreen) }
            is PlatformViewModel.UiState.Error   -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(s.message, color = PMRed) }
            is PlatformViewModel.UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LiveHelp, null, tint = PMMuted, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No FAQs yet", fontSize = 16.sp, color = PMTxt, fontWeight = FontWeight.Medium)
                            Text("Tap + to create the first FAQ", fontSize = 13.sp, color = PMMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(s.data) { faq -> FaqCard(faq = faq, onEdit = { editingFaq = faq }, onDelete = { deletingFaq = faq }) }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun FaqCard(faq: PMFaqItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isInactive = faq.isActive == false
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = PMCardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!faq.category.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(4.dp), color = PMTealBg) {
                                Text(faq.category, fontSize = 10.sp, color = PMTeal, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (isInactive) Surface(shape = RoundedCornerShape(4.dp), color = PMAmberBg) {
                            Text("Inactive", fontSize = 10.sp, color = PMAmber, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(faq.question, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PMTxt)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = PMGreen, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = PMRed, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = PMMuted)
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                    Text(faq.answer, fontSize = 13.sp, color = PMMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqCreateDialog(onDismiss: () -> Unit, onCreate: (PMCreateFaqRequest) -> Unit) {
    var question  by remember { mutableStateOf("") }
    var answer    by remember { mutableStateOf("") }
    var category  by remember { mutableStateOf("General") }
    var qError    by remember { mutableStateOf(false) }
    var aError    by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = PMCardBg, shape = RoundedCornerShape(20.dp),
        title = { Text("Create FAQ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PMTxt) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = question, onValueChange = { question = it; qError = false },
                    label = { Text("Question *") }, isError = qError, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = answer, onValueChange = { answer = it; aError = false },
                    label = { Text("Answer *") }, isError = aError, maxLines = 5,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = category, onValueChange = { category = it },
                    label = { Text("Category") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (question.isBlank()) { qError = true; return@Button }
                if (answer.isBlank())   { aError = true; return@Button }
                onCreate(PMCreateFaqRequest(question = question.trim(), answer = answer.trim(), category = category.trim().ifBlank { "General" }))
            }, colors = ButtonDefaults.buttonColors(containerColor = PMGreen), shape = RoundedCornerShape(10.dp)) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = PMMuted) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqEditDialog(faq: PMFaqItem, onDismiss: () -> Unit, onSave: (PMUpdateFaqRequest) -> Unit) {
    var question  by remember { mutableStateOf(faq.question) }
    var answer    by remember { mutableStateOf(faq.answer) }
    var category  by remember { mutableStateOf(faq.category ?: "General") }
    var isActive  by remember { mutableStateOf(faq.isActive != false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = PMCardBg, shape = RoundedCornerShape(20.dp),
        title = { Text("Edit FAQ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PMTxt) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Question") }, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Answer") }, maxLines = 5,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Active", fontSize = 14.sp, color = PMTxt)
                    Switch(checked = isActive, onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PMGreen))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(PMUpdateFaqRequest(question = question.trim().ifBlank { null }, answer = answer.trim().ifBlank { null }, category = category.trim().ifBlank { null }, isActive = isActive)) },
                colors = ButtonDefaults.buttonColors(containerColor = PMGreen), shape = RoundedCornerShape(10.dp)) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = PMMuted) } }
    )
}
