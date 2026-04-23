package com.example.weblinkscanner.ui.screens

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
    repository:    WeblinkScannerRepository,
    scanViewModel: ScanViewModel,
    userId:        String,
    onBack:        () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var links         by remember { mutableStateOf<List<SavedLinkItem>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }
    var selectedIds   by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isRescanning  by remember { mutableStateOf(false) }
    var rescanStatus  by remember { mutableStateOf<String?>(null) }
    var showRemoveDlg by remember { mutableStateOf(false) }

    fun loadLinks() {
        scope.launch {
            isLoading = true
            errorMsg  = null
            when (val r = repository.getSavedLinks(userId)) {
                is Result.Success -> links = r.data.links
                is Result.Error   -> errorMsg = "Could not load saved links."
            }
            isLoading = false
        }
    }

    LaunchedEffect(userId) { loadLinks() }

    // Remove selected dialog
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
                        loadLinks()
                    }
                    showRemoveDlg = false
                }) {
                    Text("Remove", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDlg = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

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
                "Saved links can be re-checked to see if the risk level changed.",
                fontSize  = 13.sp,
                color     = TextMuted,
                modifier  = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Rescan status message
            rescanStatus?.let {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = GreenBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = GreenSafe, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, fontSize = 13.sp, color = GreenSafe, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Saved Links List card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Saved Links List",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    when {
                        isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            CircularProgressIndicator(color = Blue600)
                        }

                        errorMsg != null -> Text(
                            errorMsg!!, color = RedDanger, fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )

                        links.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text(
                                "No saved links yet.\nSave a link after scanning to see it here.",
                                color    = TextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
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

            // Re-check All button — just refreshes from DB, does NOT consume scan quota
            Button(
                onClick  = {
                    if (isRescanning) return@Button
                    isRescanning  = true
                    rescanStatus  = null
                    scope.launch {
                        loadLinks()
                        rescanStatus = "Links refreshed successfully."
                        isRescanning = false
                    }
                },
                enabled  = links.isNotEmpty() && !isRescanning,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Blue600,
                    disabledContainerColor = DividerCol
                )
            ) {
                if (isRescanning) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Re-checking...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-check All", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Remove Selected button
            OutlinedButton(
                onClick  = { showRemoveDlg = true },
                enabled  = selectedIds.isNotEmpty(),
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

@Composable
private fun SavedLinkRow(
    link: SavedLinkItem, isSelected: Boolean, onToggle: () -> Unit
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
                // Risk badge
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
                    text     = "Last checked: ${formatDate(link.lastCheckedAt)}",
                    fontSize = 11.sp,
                    color    = TextMuted
                )
            }
        }
        Checkbox(
            checked         = isSelected,
            onCheckedChange = { onToggle() },
            colors          = CheckboxDefaults.colors(checkedColor = Blue600, uncheckedColor = TextMuted)
        )
    }
}
