package com.example.weblinkscanner.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.weblinkscanner.data.repository.Result
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import com.example.weblinkscanner.utils.WarningStrictnessManager
import kotlinx.coroutines.launch

private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val RedDanger   = Color(0xFFDC2626)
private val RedBg       = Color(0xFFFEE2E2)
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)
private val GreenSafe   = Color(0xFF16A34A)
private val GreenBg     = Color(0xFFDCFCE7)

// Holds the result of a pre-navigation scan
private data class PendingScanResult(
    val url: String,
    val verdict: String,       // SAFE / SUSPICIOUS / DANGEROUS
    val reasons: List<String>,
    val proceed: () -> Unit,   // callback to actually load the URL
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowseScanScreen(
    repository: WeblinkScannerRepository,
    userId: String,
    userPlan: String = "standard",
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val strictness by produceState(
        initialValue = WarningStrictnessManager.get(context, userId),
        key1 = lifecycleOwner.lifecycle.currentState
    ) {
        value = WarningStrictnessManager.get(context, userId)
    }

    // Address bar state
    var addressText by remember { mutableStateOf("https://google.com") }
    var currentUrl   by remember { mutableStateOf("") }
    var pageLoading  by remember { mutableStateOf(false) }

    // Scan overlay state
    var scanning     by remember { mutableStateOf(false) }
    var pendingResult by remember { mutableStateOf<PendingScanResult?>(null) }
    var blockedCount  by remember { mutableStateOf(0) }

    // Hold a reference to the WebView so we can call loadUrl on it
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // --- Pre-navigation scan logic ---
    // Returns true if the URL should be loaded immediately (SAFE + strict=Low),
    // false if it should wait for user decision via the overlay.
    fun scanAndDecide(url: String, doLoad: () -> Unit) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            doLoad()
            return
        }
        scanning = true
        scope.launch {
            val result = repository.scanUrl(url, userId)
            scanning = false
            when (result) {
                is Result.Success -> {
                    val verdict  = result.data.riskLevel.uppercase()
                    val reasons  = result.data.threatCategories

                    val shouldBlock = verdict in listOf("SUSPICIOUS", "DANGEROUS")

                    if (!shouldBlock) {
                        doLoad()
                    } else {
                        pendingResult = PendingScanResult(
                            url     = url,
                            verdict = verdict,
                            reasons = reasons,
                            proceed = {
                                pendingResult = null
                                doLoad()
                            }
                        )
                    }
                }
                is Result.Error -> {
                    // On scan error, allow navigation (fail open)
                    doLoad()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PageBgTop, PageBgBot)))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Top bar ---
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Blue600)
                        }

                        OutlinedTextField(
                            value         = addressText,
                            onValueChange = { addressText = it },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                            placeholder   = { Text("Enter URL", color = TextMuted, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction    = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(onGo = {
                                val url = normaliseUrl(addressText)
                                addressText = url
                                scanAndDecide(url) { webViewRef?.loadUrl(url) }
                            }),
                            shape  = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Blue600,
                                unfocusedBorderColor = DividerCol,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary,
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        )

                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = {
                            val url = normaliseUrl(addressText)
                            addressText = url
                            scanAndDecide(url) { webViewRef?.loadUrl(url) }
                        }) {
                            Icon(Icons.Default.Search, null, tint = Blue600)
                        }
                    }

                    // Stats bar
                    if (blockedCount > 0 || currentUrl.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text     = if (currentUrl.isNotBlank()) currentUrl.take(40) + if (currentUrl.length > 40) "…" else "" else "",
                                fontSize = 11.sp,
                                color    = TextMuted,
                                maxLines = 1
                            )
                            if (blockedCount > 0) {
                                Text(
                                    text      = "🛡 $blockedCount blocked",
                                    fontSize  = 11.sp,
                                    color     = RedDanger,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Page loading bar
            if (pageLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color    = Blue600,
                    trackColor = DividerCol
                )
            }

            // --- WebView ---
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewRef = this

                            // Load default page on launch
                            loadUrl("https://google.com")

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // Only intercept http/https navigations
                                    if (!url.startsWith("http")) return false
                                    // Scan before loading
                                    scanAndDecide(url) { view?.loadUrl(url) }
                                    return true  // we handle it
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    pageLoading = true
                                    currentUrl  = url ?: ""
                                    addressText = url ?: addressText
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageLoading = false
                                    currentUrl  = url ?: ""
                                    addressText = url ?: addressText
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Scanning spinner overlay
                if (scanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape  = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Blue600, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Scanning link…", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Checking for threats", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }

                // Verdict overlay
                pendingResult?.let { pr ->
                    val isDANGEROUS  = pr.verdict == "DANGEROUS"
                    val isSUSPICIOUS = pr.verdict == "SUSPICIOUS"

                    val bgColor     = if (isDANGEROUS) RedBg else AmberBg
                    val accentColor = if (isDANGEROUS) RedDanger else AmberWarn
                    val icon        = if (isDANGEROUS) Icons.Default.Block else Icons.Default.Warning
                    val title       = if (isDANGEROUS) "Dangerous Link Blocked" else "Suspicious Link Warning"
                    val subtitle    = if (isDANGEROUS)
                        "This link was flagged as dangerous. We recommend not proceeding."
                    else
                        "This link shows suspicious signals. Proceed with caution."

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier  = Modifier.fillMaxWidth().padding(24.dp),
                            shape     = RoundedCornerShape(20.dp),
                            colors    = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {

                                // Icon + title
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(bgColor, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                        Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
                                            Text(
                                                pr.verdict,
                                                fontSize  = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color     = accentColor,
                                                modifier  = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Text(subtitle, fontSize = 13.sp, color = TextMuted)

                                // URL
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape  = RoundedCornerShape(8.dp),
                                    color  = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        pr.url.take(80) + if (pr.url.length > 80) "…" else "",
                                        fontSize = 11.sp,
                                        color    = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                // Reasons
                                if (pr.reasons.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("Detected signals:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    pr.reasons.take(3).forEach { reason ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text("• ", fontSize = 12.sp, color = accentColor)
                                            Text(reason, fontSize = 12.sp, color = TextMuted)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                // Action buttons
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick  = { pendingResult = null },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(10.dp),
                                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
                                    ) {
                                        Text("Go Back", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }

                                    if (strictness == "LOW") {
                                        Button(
                                            onClick = {
                                                blockedCount++
                                                pr.proceed()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape    = RoundedCornerShape(10.dp),
                                            colors   = ButtonDefaults.buttonColors(
                                                containerColor = if (isDANGEROUS) RedDanger else AmberWarn
                                            )
                                        ) {
                                            Text("Proceed Anyway", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                                        }
                                    }
                                }

                                // If not Low strictness, show message instead of proceed button
                                if (strictness != "LOW") {
                                    Spacer(Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                null,
                                                tint     = TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "To proceed to flagged links, change Warning Strictness to Low in Settings.",
                                                fontSize   = 11.sp,
                                                color      = TextMuted,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Bottom info bar ---
            Surface(shadowElevation = 4.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Blue600, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Auto-scan active", fontSize = 12.sp, color = Blue600, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Strictness: $strictness",
                        fontSize = 11.sp,
                        color    = TextMuted
                    )
                }
            }
        }
    }
}

private fun normaliseUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://")  -> trimmed
        trimmed.startsWith("https://") -> trimmed
        trimmed.isBlank()              -> "https://google.com"
        else                           -> "https://$trimmed"
    }
}
