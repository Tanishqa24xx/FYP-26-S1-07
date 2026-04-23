package com.example.weblinkscanner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val RedDanger   = Color(0xFFDC2626)

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String
)

@Composable
fun HelpFaqScreen(
    repository: WeblinkScannerRepository,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var faqs        by remember { mutableStateOf<List<FaqItem>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedIdx by remember { mutableStateOf<Int?>(null) }

    // Load FAQs from Supabase via repository
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = repository.getHelpFaqs()
                faqs      = result
                isLoading = false
            } catch (e: Exception) {
                errorMsg  = "Could not load FAQs."
                isLoading = false
            }
        }
    }

    val filtered = faqs.filter {
        searchQuery.isBlank() ||
        it.question.contains(searchQuery, ignoreCase = true) ||
        it.answer.contains(searchQuery, ignoreCase = true)
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
                Icon(Icons.Default.Help, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("Help / FAQ", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text("Find answers to common questions", fontSize = 14.sp, color = TextMuted)
            Spacer(Modifier.height(24.dp))

            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it; expandedIdx = null },
                placeholder   = { Text("Search help topics", color = TextMuted) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = Blue600) },
                trailingIcon  = if (searchQuery.isNotBlank()) ({
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = TextMuted)
                    }
                }) else null,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Blue600,
                    unfocusedBorderColor = DividerCol,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = Blue600
                )
            )

            Spacer(Modifier.height(16.dp))

            // FAQ list
            when {
                isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Blue600)
                }

                errorMsg != null -> Text(errorMsg!!, color = RedDanger, fontSize = 13.sp)

                filtered.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("No results found.", color = TextMuted, fontSize = 14.sp)
                }

                else -> {
                    filtered.forEachIndexed { index, faq ->
                        FaqCard(
                            faq        = faq,
                            isExpanded = expandedIdx == index,
                            onClick    = {
                                expandedIdx = if (expandedIdx == index) null else index
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Blue600)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem, isExpanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = faq.question,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint     = Blue600,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = DividerCol)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text     = faq.answer,
                        fontSize = 13.sp,
                        color    = TextMuted,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
