package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.models.NewScanResponse
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import kotlinx.coroutines.launch

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

@Composable
fun ScanHistoryScreen(
    repository: WeblinkScannerRepository,
    userId: String,
    userPlan: String = "free",
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val isPaidPlan = userPlan.lowercase() in listOf("standard", "premium")

    var items by remember { mutableStateOf<List<NewScanResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearAll by remember { mutableStateOf(false) }
    var showClearSel by remember { mutableStateOf(false) }

    // Search and filter state - Standard and Premium only
    var searchQuery by remember { mutableStateOf("") }
    var filterVerdict by remember { mutableStateOf("ALL") } // ALL, SAFE, SUSPICIOUS, DANGEROUS

    // Load history on open
    LaunchedEffect(userId) {
        isLoading = true
        errorMsg  = null
        when (val r = repository.getScanHistory(userId)) {
            is Result.Success -> items = r.data
            is Result.Error   -> errorMsg = "Could not load history."
        }
        isLoading = false
    }

    // Derived filtered list (search + verdict filter, paid plans only)
    val displayItems = remember(items, searchQuery, filterVerdict) {
        items.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    (item.url ?: "").contains(searchQuery, ignoreCase = true) ||
                    item.riskLevel.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filterVerdict == "ALL" ||
                    item.riskLevel.uppercase() == filterVerdict
            matchesSearch && matchesFilter
        }
    }

    // --- Dialogs ---
    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            title = { Text("Clear All History", fontWeight = FontWeight.Bold) },
            text  = { Text("This will remove all scan history from this view.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    val ids = items.map { it.scanId }
                    items = emptyList(); selectedIds = emptySet(); showClearAll = false
                    scope.launch { repository.deleteHistoryItems(ids) }
                }) {
                    Text("Clear All", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    if (showClearSel) {
        AlertDialog(
            onDismissRequest = { showClearSel = false },
            title = { Text("Clear Selected", fontWeight = FontWeight.Bold) },
            text  = { Text("Remove ${selectedIds.size} selected item(s)?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    val idsToDelete = selectedIds.toList()
                    items = items.filter { it.scanId !in selectedIds }
                    selectedIds = emptySet()
                    showClearSel = false
                    scope.launch { repository.deleteHistoryItems(idsToDelete) }
                }) {
                    Text("Clear", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSel = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    // --- Layout ---
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

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.History, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("Scan History", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text("Your recent URL scans", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(24.dp))

            // Search bar - Standard and Premium only
            if (isPaidPlan) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search by URL or verdict", color = TextMuted) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = Blue600) },
                    trailingIcon  = if (searchQuery.isNotBlank()) ({
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = TextMuted)
                        }
                    }) else null,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Blue600,
                        unfocusedBorderColor = DividerCol,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = Blue600
                    )
                )
                Spacer(Modifier.height(10.dp))

                // Verdict filter chips
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "SAFE", "SUSPICIOUS", "DANGEROUS").forEach { verdict ->
                        val isActive = filterVerdict == verdict
                        val chipColor = when (verdict) {
                            "SAFE"      -> Color(0xFF16A34A)
                            "SUSPICIOUS"-> Color(0xFFD97706)
                            "DANGEROUS" -> Color(0xFFDC2626)
                            else        -> Blue600
                        }
                        FilterChip(
                            selected = isActive,
                            onClick  = { filterVerdict = verdict },
                            label    = { Text(verdict, fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.15f),
                                selectedLabelColor     = chipColor
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // List card - uses displayItems (filtered for paid, raw for free)
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when {
                        isLoading -> Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            Alignment.Center
                        ) { CircularProgressIndicator(color = Blue600) }

                        errorMsg != null -> Text(
                            errorMsg!!, color = RedDanger, fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )

                        displayItems.isEmpty() -> Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            Alignment.Center
                        ) {
                            Text(
                                if (items.isEmpty()) "No scan history yet.\nScan a URL to get started."
                                else "No results match your search.",
                                color     = TextMuted,
                                fontSize  = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        else -> displayItems.forEachIndexed { index, item ->
                            HistoryRow(
                                url        = item.url ?: "",
                                riskLevel  = item.riskLevel,
                                date       = item.scannedAt.take(10),
                                isSelected = item.scanId in selectedIds,
                                onToggle   = {
                                    selectedIds = if (item.scanId in selectedIds)
                                        selectedIds - item.scanId
                                    else
                                        selectedIds + item.scanId
                                }
                            )
                            if (index < displayItems.lastIndex)
                                HorizontalDivider(color = DividerCol, thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick  = { showClearSel = true },
                enabled  = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Blue600,
                    disabledContainerColor = DividerCol
                )
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (selectedIds.isEmpty()) "Clear Selected"
                    else "Clear Selected (${selectedIds.size})",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick  = { showClearAll = true },
                enabled  = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger)
            ) {
                Icon(Icons.Default.Delete, null,
                    tint     = if (items.isNotEmpty()) RedDanger else TextMuted,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear All", fontWeight = FontWeight.SemiBold,
                    color = if (items.isNotEmpty()) RedDanger else TextMuted)
            }

            Spacer(Modifier.height(10.dp))

            // Export button — Standard (CSV only) and Premium (CSV + PDF)
            if (isPaidPlan) {
                val isPremium = userPlan.lowercase() == "premium"
                var showExportMenu by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val baseUrl = "http://10.0.2.2:8000/scan/export"

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick  = { showExportMenu = true },
                        enabled  = items.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                    ) {
                        Icon(
                            Icons.Default.Share, null,
                            tint     = if (items.isNotEmpty()) Blue600 else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Export History",
                            fontWeight = FontWeight.SemiBold,
                            color      = if (items.isNotEmpty()) Blue600 else TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded        = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text    = { Text("Export as CSV") },
                            onClick = {
                                showExportMenu = false
                                val url = "$baseUrl?user_id=$userId&fmt=csv"
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(url)
                                )
                                context.startActivity(intent)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.History, null, tint = Blue600, modifier = Modifier.size(18.dp))
                            }
                        )
                        if (isPremium) {
                            DropdownMenuItem(
                                text    = { Text("Export as PDF") },
                                onClick = {
                                    showExportMenu = false
                                    val url = "$baseUrl?user_id=$userId&fmt=pdf"
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url)
                                    )
                                    context.startActivity(intent)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.History, null, tint = Blue600, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

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

@Composable
private fun HistoryRow(
    url: String, riskLevel: String, date: String,
    isSelected: Boolean, onToggle: () -> Unit
) {
    val (badgeColor, badgeBg) = when (riskLevel.uppercase()) {
        "SAFE"       -> Pair(GreenSafe, GreenBg)
        "SUSPICIOUS" -> Pair(AmberWarn, AmberBg)
        "DANGEROUS"  -> Pair(RedDanger, RedBg)
        else         -> Pair(TextMuted,  Blue50)
    }
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(riskLevel.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(url, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (date.isNotBlank())
                Text(date, fontSize = 11.sp, color = TextMuted)
        }
        Checkbox(
            checked         = isSelected,
            onCheckedChange = { onToggle() },
            colors          = CheckboxDefaults.colors(checkedColor = Blue600, uncheckedColor = TextMuted)
        )
    }
}
