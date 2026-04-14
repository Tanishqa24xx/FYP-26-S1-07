package com.example.weblinkscanner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.RecheckUrlItem
import com.example.weblinkscanner.data.models.SavedLinkItem
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import com.example.weblinkscanner.viewmodel.ScanViewModel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val GreenSafe   = Color(0xFF16A34A)
private val GreenBg     = Color(0xFFDCFCE7)
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)
private val RedDanger   = Color(0xFFDC2626)
private val RedBg       = Color(0xFFFEE2E2)

private fun formatDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val zdt = ZonedDateTime.parse(raw)
        zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", Locale.getDefault()))
    } catch (e: Exception) {
        raw.take(16).replace("T", ", ")
    }
}

@Composable
fun SavedLinksScreen(
    repository: WeblinkScannerRepository,
    scanViewModel: ScanViewModel,
    userId: String,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // --- State ---
    var links         by remember { mutableStateOf<List<SavedLinkItem>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var isRefreshing  by remember { mutableStateOf(false) }
    var isRechecking  by remember { mutableStateOf(false) }
    var recheckProgress by remember { mutableStateOf(0) }   // how many URLs done
    var recheckTotal  by remember { mutableStateOf(0) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }
    var statusMsg     by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var selectedIds   by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showRemoveDlg by remember { mutableStateOf(false) }

    // --- Helpers ---

    /** Reloads the saved-links list from the database. No scanning. */
    fun refresh() {
        scope.launch {
            isRefreshing = true
            errorMsg     = null
            when (val r = repository.getSavedLinks(userId)) {
                is Result.Success -> {
                    links     = r.data.links
                    statusMsg = "List refreshed."
                    statusIsError = false
                }
                is Result.Error -> errorMsg = "Could not load saved links."
            }
            isRefreshing = false
        }
    }

    /**
     * Actually re-scans every saved link via the backend /saved-links/recheck
     * endpoint. Updates risk_level + last_checked_at in the database and
     * reflects the new verdict in the UI immediately.
     */
    fun recheckAll() {
        if (links.isEmpty()) return
        scope.launch {
            isRechecking    = true
            statusMsg       = null
            recheckProgress = 0
            recheckTotal    = links.size

            val recheckItems = links.map { RecheckUrlItem(id = it.id, url = it.url) }

            when (val r = repository.recheckSavedLinks(userId, recheckItems)) {
                is Result.Success -> {
                    val resultMap = r.data.results.associateBy { it.id }
                    // Merge new verdicts into the displayed list
                    links = links.map { link ->
                        val updated = resultMap[link.id]
                        if (updated != null) {
                            link.copy(
                                riskLevel       = updated.newRiskLevel,
                                lastCheckedAt   = updated.lastCheckedAt
                            )
                        } else link
                    }
                    recheckProgress = links.size
                    val errorCount  = r.data.errors.size
                    statusMsg = if (errorCount == 0)
                        "All ${links.size} link(s) re-checked successfully."
                    else
                        "${links.size - errorCount} link(s) updated. $errorCount failed."
                    statusIsError = errorCount > 0
                }
                is Result.Error -> {
                    statusMsg     = "Re-check failed: ${r.message}"
                    statusIsError = true
                }
            }
            isRechecking = false
        }
    }

    // Load on entry
    LaunchedEffect(userId) {
        isLoading = true
        when (val r = repository.getSavedLinks(userId)) {
            is Result.Success -> links = r.data.links
            is Result.Error   -> errorMsg = "Could not load saved links."
        }
        isLoading = false
    }

    // --- Remove dialog ---
    if (showRemoveDlg) {
        AlertDialog(
            onDismissRequest = { showRemoveDlg = false },
            title = { Text("Remove Selected", fontWeight = FontWeight.Bold) },
            text  = { Text("Remove ${selectedIds.size} saved link(s)?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteLinks(selectedIds.toList())
                        selectedIds = emptySet()
                        refresh()
                    }
                    showRemoveDlg = false
                }) {
                    Text("Remove", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDlg = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageBgTop, PageBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bookmark, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Saved Links", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text(
                "Save links after scanning. Re-check them anytime to see if their risk level has changed.",
                fontSize  = 13.sp,
                color     = TextMuted,
                modifier  = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // --- Status banner ---
            AnimatedVisibility(visible = statusMsg != null) {
                statusMsg?.let { msg ->
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = if (statusIsError) RedBg else GreenBg
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (statusIsError) Icons.Default.ErrorOutline
                                else Icons.Default.CheckCircle,
                                null,
                                tint     = if (statusIsError) RedDanger else GreenSafe,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                msg,
                                fontSize   = 13.sp,
                                color      = if (statusIsError) RedDanger else GreenSafe,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // --- Re-check progress indicator ---
            AnimatedVisibility(visible = isRechecking) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = Blue50),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color     = Blue600,
                                modifier  = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Re-scanning links against live threat feeds…",
                                fontSize = 13.sp,
                                color    = Blue600,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "This may take a moment. Each link is checked against URLhaus, PhishTank, and heuristic signals.",
                            fontSize   = 11.sp,
                            color      = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // --- Saved Links list card ---
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Saved Links",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        if (links.isNotEmpty()) {
                            Text(
                                "${links.size} link${if (links.size == 1) "" else "s"}",
                                fontSize = 12.sp,
                                color    = TextMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    when {
                        isLoading -> Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            Alignment.Center
                        ) { CircularProgressIndicator(color = Blue600) }

                        errorMsg != null -> Text(
                            errorMsg!!,
                            color    = RedDanger,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )

                        links.isEmpty() -> Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.BookmarkBorder,
                                    null,
                                    tint     = TextMuted.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No saved links yet.\nSave a link after scanning to see it here.",
                                    color     = TextMuted,
                                    fontSize  = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        else -> links.forEachIndexed { index, link ->
                            SavedLinkRow(
                                link       = link,
                                isSelected = link.id in selectedIds,
                                onToggle   = {
                                    selectedIds = if (link.id in selectedIds)
                                        selectedIds - link.id
                                    else
                                        selectedIds + link.id
                                }
                            )
                            if (index < links.lastIndex)
                                HorizontalDivider(color = DividerCol, thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- Re-check All button ---
            // Actually re-scans every saved URL against live threat intelligence.
            Button(
                onClick  = { recheckAll() },
                enabled  = links.isNotEmpty() && !isRechecking && !isRefreshing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Blue600,
                    disabledContainerColor = DividerCol
                )
            ) {
                if (isRechecking) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Re-checking…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.ManageSearch, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-check All Links", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Refresh button ---
            // Reloads records from the database
            OutlinedButton(
                onClick  = { statusMsg = null; refresh() },
                enabled  = !isRefreshing && !isRechecking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Blue600
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Refreshing…", fontWeight = FontWeight.SemiBold, color = Blue600)
                } else {
                    Icon(Icons.Default.Refresh, null, tint = Blue600, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh List", fontWeight = FontWeight.SemiBold, color = Blue600)
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Remove Selected button ---
            OutlinedButton(
                onClick  = { showRemoveDlg = true },
                enabled  = selectedIds.isNotEmpty() && !isRechecking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger)
            ) {
                Icon(
                    Icons.Default.Delete, null,
                    tint     = if (selectedIds.isNotEmpty()) RedDanger else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (selectedIds.isEmpty()) "Remove Selected"
                    else "Remove Selected (${selectedIds.size})",
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selectedIds.isNotEmpty()) RedDanger else TextMuted
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back", fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// --- Row composable ---

@Composable
private fun SavedLinkRow(
    link: SavedLinkItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val (badgeColor, badgeBg) = when (link.riskLevel?.uppercase()) {
        "SAFE"       -> Pair(GreenSafe, GreenBg)
        "SUSPICIOUS" -> Pair(AmberWarn, AmberBg)
        "DANGEROUS"  -> Pair(RedDanger, RedBg)
        else         -> Pair(TextMuted,  Blue50)
    }

    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = link.url,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text       = link.riskLevel?.uppercase() ?: "UNKNOWN",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = badgeColor
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = "Checked: ${formatDate(link.lastCheckedAt)}",
                    fontSize = 11.sp,
                    color    = TextMuted
                )
            }
        }
        Checkbox(
            checked         = isSelected,
            onCheckedChange = { onToggle() },
            colors          = CheckboxDefaults.colors(
                checkedColor   = Blue600,
                uncheckedColor = TextMuted
            )
        )
    }
}