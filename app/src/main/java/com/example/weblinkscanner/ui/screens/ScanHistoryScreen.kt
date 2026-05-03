package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
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

private enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    repository: WeblinkScannerRepository,
    userId: String,
    userPlan: String = "free",
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val isPaidPlan = userPlan.lowercase() in listOf("standard", "premium")
    val isPremium  = userPlan.lowercase() == "premium"

    var items       by remember { mutableStateOf<List<NewScanResponse>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearAll by remember { mutableStateOf(false) }
    var showClearSel by remember { mutableStateOf(false) }

    var searchQuery   by remember { mutableStateOf("") }
    var filterVerdict by remember { mutableStateOf("ALL") }
    var sortOrder     by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var showExportMenu by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(userId) {
        isLoading = true; errorMsg = null
        when (val r = repository.getScanHistory(userId)) {
            is Result.Success -> items = r.data
            is Result.Error   -> errorMsg = "Could not load history."
        }
        isLoading = false
    }

    val displayItems = remember(items, searchQuery, filterVerdict, sortOrder) {
        val filtered = items.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    (item.url ?: "").contains(searchQuery, ignoreCase = true) ||
                    item.riskLevel.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filterVerdict == "ALL" ||
                    item.riskLevel.uppercase() == filterVerdict
            matchesSearch && matchesFilter
        }
        if (isPaidPlan) {
            when (sortOrder) {
                SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.scannedAt }
                SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.scannedAt }
            }
        } else filtered
    }

    // Dialogs
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
                }) { Text("Clear All", color = RedDanger, fontWeight = FontWeight.Bold) }
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
                    selectedIds = emptySet(); showClearSel = false
                    scope.launch { repository.deleteHistoryItems(idsToDelete) }
                }) { Text("Clear", color = RedDanger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearSel = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    // Scaffold with pinned bottom bar
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = CardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Clear Selected + Clear All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clear Selected
                        Button(
                            onClick  = { showClearSel = true },
                            enabled  = selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = Blue600,
                                disabledContainerColor = DividerCol
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (selectedIds.isEmpty()) "Clear Selected"
                                else "(${selectedIds.size})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        // Clear All
                        OutlinedButton(
                            onClick  = { showClearAll = true },
                            enabled  = items.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger)
                        ) {
                            Icon(Icons.Default.Delete, null,
                                tint     = if (items.isNotEmpty()) RedDanger else TextMuted,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear All", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                color = if (items.isNotEmpty()) RedDanger else TextMuted)
                        }
                    }

                    // Row 2: Export (paid only) + Back
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export button — Standard & Premium only
                        if (isPaidPlan) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick  = { showExportMenu = true },
                                    enabled  = items.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                                ) {
                                    Icon(Icons.Default.Share, null,
                                        tint     = if (items.isNotEmpty()) Blue600 else TextMuted,
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Export", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                        color = if (items.isNotEmpty()) Blue600 else TextMuted)
                                }
                                DropdownMenu(
                                    expanded         = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    val baseUrl = "http://10.0.2.2:8000/scan/export"
                                    DropdownMenuItem(
                                        text = { Text("Export as CSV") },
                                        onClick = {
                                            showExportMenu = false
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("$baseUrl?user_id=$userId&fmt=csv")
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, null, tint = Blue600,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as PDF") },
                                        onClick = {
                                            showExportMenu = false
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("$baseUrl?user_id=$userId&fmt=pdf")
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.History, null, tint = Blue600,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }

                        // Back button
                        OutlinedButton(
                            onClick  = onBack,
                            modifier = Modifier
                                .then(if (isPaidPlan) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                .height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Blue600,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Blue600)
                        }
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(Brush.verticalGradient(listOf(PageBgTop, PageBgBot)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
            ) {
                // Header
                item {
                    Spacer(Modifier.height(36.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Brush.radialGradient(listOf(Blue100, Blue50)),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, null, tint = Blue600,
                                modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Scan History", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = Blue600, modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center)
                    Text("Your recent URL scans", fontSize = 14.sp, color = TextMuted,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                }

                // Search + Filter + Sort (paid only)
                if (isPaidPlan) {
                    item {
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

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ALL", "SAFE", "SUSPICIOUS", "DANGEROUS").forEach { verdict ->
                                    val isActive = filterVerdict == verdict
                                    val chipColor = when (verdict) {
                                        "SAFE"       -> GreenSafe
                                        "SUSPICIOUS" -> AmberWarn
                                        "DANGEROUS"  -> RedDanger
                                        else         -> Blue600
                                    }
                                    FilterChip(
                                        selected = isActive,
                                        onClick  = { filterVerdict = verdict },
                                        label    = { Text(verdict, fontSize = 10.sp) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = chipColor.copy(alpha = 0.15f),
                                            selectedLabelColor     = chipColor
                                        )
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    sortOrder = if (sortOrder == SortOrder.NEWEST_FIRST)
                                        SortOrder.OLDEST_FIRST else SortOrder.NEWEST_FIRST
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (sortOrder == SortOrder.NEWEST_FIRST)
                                        Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Blue600, modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sort, null, tint = TextMuted,
                                modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (sortOrder == SortOrder.NEWEST_FIRST) "Newest first" else "Oldest first",
                                fontSize = 11.sp, color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // History list card
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {

                        when {
                            isLoading -> Box(
                                Modifier.fillMaxWidth().padding(32.dp), Alignment.Center
                            ) { CircularProgressIndicator(color = Blue600) }

                            errorMsg != null -> Text(
                                errorMsg!!, color = RedDanger, fontSize = 13.sp,
                                modifier = Modifier.padding(8.dp)
                            )

                            displayItems.isEmpty() -> Box(
                                Modifier.fillMaxWidth().padding(32.dp), Alignment.Center
                            ) {
                                Text(
                                    if (items.isEmpty()) "No scan history yet.\nScan a URL to get started."
                                    else "No results match your search.",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            else -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                displayItems.forEachIndexed { index, item ->
                                    HistoryRow(
                                        url = item.url ?: "",
                                        riskLevel = item.riskLevel,
                                        date = item.scannedAt.take(10),
                                        isSelected = item.scanId in selectedIds,
                                        onToggle = {
                                            selectedIds = if (item.scanId in selectedIds)
                                                selectedIds - item.scanId
                                            else
                                                selectedIds + item.scanId
                                        }
                                    )
                                    if (index < displayItems.lastIndex)
                                        HorizontalDivider(color = DividerCol, thickness = 0.5.dp)
                                } // rows rendered below as individual items
                            }
                        }
                    }
                }

                // Bottom spacer so last item isn't hidden behind bottom bar
                item { Spacer(Modifier.height(16.dp)) }
            }
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