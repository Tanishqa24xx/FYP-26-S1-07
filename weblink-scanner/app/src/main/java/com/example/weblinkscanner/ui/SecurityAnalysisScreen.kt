// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\SecurityAnalysisScreen.kt

// Shows: header description, URL analysed, Checks card, Details card, Back to result

package com.example.weblinkscanner.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weblinkscanner.viewmodel.SandboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAnalysisScreen(
    viewModel: SandboxViewModel,
    url: String,
    scanId: String,
    onBack: () -> Unit
) {
    val report  by viewModel.sandboxReport.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error   by viewModel.error.collectAsState()

    // Trigger analysis if not already done
    LaunchedEffect(Unit) {
        if (report == null) viewModel.analyseSandbox(url, scanId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sandbox Security analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Header description
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Standard security analysis (Free)", fontWeight = FontWeight.SemiBold)
                    Text("Shows basic checks to help you decide.", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (loading) {
                CircularProgressIndicator()
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // URL analysed
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "URL analysed: $url",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Checks card — populated from sandbox report if available
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Checks", fontWeight = FontWeight.SemiBold)

                    val r = report
                    if (r != null) {
                        val httpsOk = url.startsWith("https")
                        Text("• HTTPS / certificate: ${if (httpsOk) "Pass" else "Fail"}")
                        Text("• Domain age: Unknown")
                        Text("• Redirects detected: ${if (r.redirectChain.size > 1) "Yes" else "No"}")
                        Text("• Known blacklist match: Unknown")
                        Text("• Basic reputation: Neutral")
                    } else {
                        Text("• HTTPS / certificate: Pass/Fail")
                        Text("• Domain age: New/Old/Unknown")
                        Text("• Redirects detected: Yes/No")
                        Text("• Known blacklist match: Yes/No")
                        Text("• Basic reputation: Good/Neutral/Bad")
                    }
                }
            }

            // Details card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Details", fontWeight = FontWeight.SemiBold)

                    val r = report
                    if (r != null) {
                        Text("• Final destination URL: ${r.redirectChain.lastOrNull() ?: url}")
                        if (r.redirectChain.size > 1) {
                            Text("• Redirect chain:")
                            r.redirectChain.forEach { Text("  → $it", style = MaterialTheme.typography.bodySmall) }
                        } else {
                            Text("• Redirect chain: None")
                        }
                        Text("• Notes / warnings: ${if (r.externalLinks.isNotEmpty()) "${r.externalLinks.size} external links found" else "None"}")
                    } else {
                        Text("• Final destination URL")
                        Text("• Redirect chain")
                        Text("• Notes / warnings")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to result")
            }
        }
    }
}