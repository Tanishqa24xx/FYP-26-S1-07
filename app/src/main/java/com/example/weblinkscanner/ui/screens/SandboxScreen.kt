package com.example.weblinkscanner.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.weblinkscanner.data.models.SandboxReport
import com.example.weblinkscanner.viewmodel.SandboxViewModel

// ---------------------------------------------------------------------------
// Colour palette
// ---------------------------------------------------------------------------
private val Blue600     = Color(0xFF2563EB)
private val Blue50      = Color(0xFFEFF6FF)
private val Blue100     = Color(0xFFDBEAFE)
private val PageBgTop   = Color(0xFFEFF6FF)
private val PageBgBot   = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted   = Color(0xFF64748B)
private val DividerCol  = Color(0xFFE2E8F0)
private val GreenPass   = Color(0xFF16A34A)
private val GreenBg     = Color(0xFFDCFCE7)
private val RedFail     = Color(0xFFDC2626)
private val RedBg       = Color(0xFFFEE2E2)
private val AmberWarn   = Color(0xFFD97706)
private val AmberBg     = Color(0xFFFEF3C7)

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------
@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel,
    url: String,
    scanId: String,
    userId: String,
    onBack: () -> Unit
) {
    val report  by viewModel.sandboxReport.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error   by viewModel.error.collectAsState()
    val context = LocalContext.current

    // Only fetch if no report exists yet, or if the URL changed.
    // Security Analysis and Sandbox share the same SandboxViewModel -
    // if Security Analysis already fetched the report, reuse it here.
    LaunchedEffect(url) {
        if (report == null || report?.url != url) {
            viewModel.analyseSandbox(url, scanId, userId)
        }
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

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(Blue100, Blue50)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BugReport, null, tint = Blue600, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Sandbox Analysis", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Text(
                url, fontSize = 12.sp, color = TextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))

            when {
                loading        -> LoadingCard()
                error != null  -> ErrorCard(error!!) { viewModel.analyseSandbox(url, scanId, userId) }
                report != null -> ReportContent(report!!, context)
            }

            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Text("Back to Result", fontWeight = FontWeight.SemiBold, color = Blue600)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------
@Composable
private fun LoadingCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Blue600)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Visiting URL in isolated browser\u2026",
                    color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your device never contacts the scanned URL.",
                    color = Blue600, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------
@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = RedBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = RedFail, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(error, color = RedFail, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(10.dp))
    Button(
        onClick  = onRetry,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
    ) {
        Text("Retry", fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Full report
// ---------------------------------------------------------------------------
@Composable
private fun ReportContent(r: SandboxReport, context: android.content.Context) {

    // --- urlscan verdict banner ---
    val score = r.verdictScore
    if (score != null || r.malicious != null) {
        val isMalicious = r.malicious == true || (score != null && score > 0)
        val (bg, textColor, icon, label) = if (isMalicious)
            listOf(RedBg, RedFail, Icons.Default.GppBad, "Flagged as malicious by urlscan.io (score: $score)")
        else
            listOf(GreenBg, GreenPass, Icons.Default.VerifiedUser, "No malicious content detected (score: $score)")
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(containerColor = bg as Color),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon as ImageVector, null, tint = textColor as Color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(label as String, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    if (r.verdictCategories.isNotEmpty()) {
                        Text(
                            r.verdictCategories.joinToString(", "),
                            fontSize = 11.sp, color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Screenshot ---
    // urlscan.io returns a public PNG URL: https://urlscan.io/screenshots/{uuid}.png
    // We load it with Coil exactly like any remote image.
    r.screenshotUrl?.let { shotUrl ->
        SectionCard(title = "Page Screenshot", icon = Icons.Default.Screenshot) {
            Text(
                "Captured by urlscan.io. Your device never loaded this page.",
                fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp)
            )
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(shotUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Website screenshot",
                contentScale       = ContentScale.FillWidth,
                modifier           = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                loading = {
                    Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Blue600, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.height(6.dp))
                            Text("Loading screenshot\u2026", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                },
                error = {
                    Box(
                        Modifier.fillMaxWidth().height(60.dp).background(AmberBg, RoundedCornerShape(8.dp)),
                        Alignment.Center
                    ) {
                        Text("Screenshot not available for this page.", fontSize = 11.sp, color = AmberWarn,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Page overview ---
    SectionCard(title = "Page Overview", icon = Icons.Default.Info) {
        DetailRow("Page Title",  r.pageTitle  ?: "-")
        Div()
        DetailRow("IP Address",  r.ipAddress  ?: "-")
        r.statusCode?.let { Div(); DetailRow("HTTP Status", "HTTP $it \u2014 ${httpText(it)}") }
        r.server?.let      { Div(); DetailRow("Server",     it) }
        r.mimeType?.let    { Div(); DetailRow("MIME Type",  it) }
        r.ptr?.let         { Div(); DetailRow("PTR Record", it) }
        r.country?.let     { Div(); DetailRow("Country",    "${r.city?.let { c -> "$c, " } ?: ""}$it") }
        r.apexDomain?.let  { Div(); DetailRow("Domain",     it) }
        r.finalUrl?.let    { if (it != r.url) { Div(); DetailRow("Final URL", it) } }
        r.loadTimeMs?.let {
            Div()
            DetailRow("Load Time", when {
                it == 0   -> "-"
                it < 2000 -> "${it}ms \u2014 fast"
                it < 5000 -> "${it}ms \u2014 moderate"
                else      -> "${it}ms \u2014 slow"
            })
        }
        r.totalRequests?.let { Div(); DetailRow("Requests",   "$it HTTP requests") }
        r.totalSizeKb?.let   { Div(); DetailRow("Page Size",  "$it KB") }
    }
    Spacer(Modifier.height(12.dp))

    // --- SSL / TLS: raw certificate fields ---
    r.sslInfo?.let { ssl ->
        val sslValid = ssl.valid == true
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = if (sslValid) GreenBg else RedBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (sslValid) Icons.Default.Lock else Icons.Default.LockOpen, null,
                        tint = if (sslValid) GreenPass else RedFail, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SSL / TLS Certificate",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (sslValid) GreenPass else RedFail
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = DividerCol)
                Spacer(Modifier.height(10.dp))
                // Show raw fields only - no explanatory text
                DetailRow("Status",     if (sslValid) "Valid" else "Invalid / not present")
                ssl.issuer?.let     { Div(); DetailRow("Issuer",      it.take(80)) }
                ssl.validFrom?.let  { Div(); DetailRow("Valid From",  it) }
                ssl.validDays?.let  { Div(); DetailRow("Valid For",   "$it days") }
                ssl.ageDays?.let    { Div(); DetailRow("Age at Scan", "$it days") }
                ssl.protocol?.let   { Div(); DetailRow("Protocol",    it) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Technologies ---
    if (r.techDetected.isNotEmpty()) {
        SectionCard(
            title    = "Technologies Detected",
            icon     = Icons.Default.Code,
            subtitle = "Frameworks and tools identified by Wappalyzer"
        ) {
            r.techDetected.forEachIndexed { i, tech ->
                if (i > 0) Div()
                Text("\u2022 $tech", fontSize = 13.sp, color = TextPrimary)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Hosting / ASN ---
    r.asnInfo?.let { asn ->
        SectionCard(title = "Hosting", icon = Icons.Default.Storage) {
            asn.asn?.let     { DetailRow("ASN",     it) }
            asn.asnname?.let { Div(); DetailRow("Network", it) }
            asn.country?.let { Div(); DetailRow("Country", it) }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Redirect chain - show first 6, expand ---
    var redirectExpanded by remember { mutableStateOf(false) }
    SectionCard(
        title    = "Redirect Chain",
        icon     = Icons.Default.SwapHoriz,
        subtitle = "Every address visited before the final page",
        badge    = if (r.redirectChain.size > 1)
            "${r.redirectChain.size} hops" to AmberWarn
        else
            "${r.redirectChain.size} hop" to GreenPass
    ) {
        if (r.redirectChain.isEmpty()) {
            Text("No redirects.", fontSize = 13.sp, color = TextMuted)
        } else {
            val visibleRedirects = if (redirectExpanded) r.redirectChain else r.redirectChain.take(6)
            visibleRedirects.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("${i + 1}.", fontSize = 12.sp, color = TextMuted, modifier = Modifier.width(20.dp))
                    Text(step, fontSize = 12.sp, color = TextPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(3.dp))
            }
            if (r.redirectChain.size > 6) {
                Div()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { redirectExpanded = !redirectExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (redirectExpanded) "Show less" else "Show ${r.redirectChain.size - 6} more",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Blue600, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (redirectExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Blue600, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // --- External links - show first 6, expand to see rest ---
    var linksExpanded by remember { mutableStateOf(false) }
    SectionCard(
        title    = "External Links",
        icon     = Icons.Default.Link,
        subtitle = "${r.externalLinks.size} link(s) found on the page",
        badge    = r.externalLinks.size.toString() to Blue600
    ) {
        if (r.externalLinks.isEmpty()) {
            Text("No external links found.", fontSize = 13.sp, color = TextMuted)
        } else {
            val visibleLinks = if (linksExpanded) r.externalLinks else r.externalLinks.take(6)
            visibleLinks.forEachIndexed { i, link ->
                if (i > 0) Div()
                Text(
                    link, fontSize = 11.sp, color = Blue600,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            if (r.externalLinks.size > 6) {
                Div()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { linksExpanded = !linksExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (linksExpanded) "Show less" else "Show ${r.externalLinks.size - 6} more",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Blue600, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (linksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Blue600, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // --- Domains contacted - show first 6, expand to see rest ---
    if (r.domainsContacted.isNotEmpty()) {
        var domainsExpanded by remember { mutableStateOf(false) }
        SectionCard(
            title    = "Domains Contacted",
            icon     = Icons.Default.Language,
            subtitle = "${r.domainsContacted.size} domain(s)",
            badge    = r.domainsContacted.size.toString() to Blue600
        ) {
            val visibleDomains = if (domainsExpanded) r.domainsContacted else r.domainsContacted.take(6)
            visibleDomains.forEachIndexed { i, domain ->
                if (i > 0) Div()
                Text("• $domain", fontSize = 12.sp, color = TextMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (r.domainsContacted.size > 6) {
                Div()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { domainsExpanded = !domainsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (domainsExpanded) "Show less" else "Show ${r.domainsContacted.size - 6} more",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Blue600, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (domainsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Blue600, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- IPs contacted - show first 6, expand ---
    if (r.ipsContacted.isNotEmpty()) {
        var ipsExpanded by remember { mutableStateOf(false) }
        SectionCard(
            title    = "IPs Contacted",
            icon     = Icons.Default.Hub,
            subtitle = "${r.ipsContacted.size} unique IP address(es)"
        ) {
            val visibleIps = if (ipsExpanded) r.ipsContacted else r.ipsContacted.take(6)
            visibleIps.forEachIndexed { i, ip ->
                if (i > 0) Div()
                Text("• $ip", fontSize = 12.sp, color = TextMuted)
            }
            if (r.ipsContacted.size > 6) {
                Div()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { ipsExpanded = !ipsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (ipsExpanded) "Show less" else "Show ${r.ipsContacted.size - 6} more",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Blue600, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (ipsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Blue600, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Console messages - show first 6, expand ---
    if (r.consoleMessages.isNotEmpty()) {
        var consoleExpanded by remember { mutableStateOf(false) }
        SectionCard(
            title    = "Browser Console",
            icon     = Icons.Default.Terminal,
            subtitle = "Errors and warnings recorded during page load"
        ) {
            val visibleMsgs = if (consoleExpanded) r.consoleMessages else r.consoleMessages.take(6)
            visibleMsgs.forEach { msg ->
                Text("› $msg", fontSize = 11.sp, color = TextMuted,
                    modifier = Modifier.padding(vertical = 1.dp))
            }
            if (r.consoleMessages.size > 6) {
                Div()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { consoleExpanded = !consoleExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (consoleExpanded) "Show less" else "Show ${r.consoleMessages.size - 6} more",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Blue600, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (consoleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Blue600, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Report link ---
    r.reportUrl?.let { rUrl ->
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(containerColor = Blue50),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.OpenInBrowser, null, tint = Blue600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Full report on urlscan.io", fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = Blue600)
                    Text(rUrl, fontSize = 11.sp, color = TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Reusable composables
// ---------------------------------------------------------------------------

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    badge: Pair<String, Color>? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Blue600, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, modifier = Modifier.weight(1f))
                badge?.let { (label, color) ->
                    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            subtitle?.let {
                Text(it, fontSize = 11.sp, color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            } ?: Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = TextMuted, modifier = Modifier.width(100.dp))
        Text(value, fontSize = 12.sp, color = TextPrimary,
            modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Div() {
    HorizontalDivider(color = DividerCol, modifier = Modifier.padding(vertical = 6.dp))
}

private fun httpText(code: Int) = when (code) {
    200 -> "OK"; 201 -> "Created"; 204 -> "No Content"
    301 -> "Moved Permanently"; 302 -> "Found"; 304 -> "Not Modified"
    400 -> "Bad Request"; 401 -> "Unauthorized"; 403 -> "Forbidden"
    404 -> "Not Found"; 429 -> "Too Many Requests"
    500 -> "Server Error"; 502 -> "Bad Gateway"; 503 -> "Unavailable"
    else -> "Status $code"
}
