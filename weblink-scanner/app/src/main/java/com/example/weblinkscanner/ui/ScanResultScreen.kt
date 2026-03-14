// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\ScanResultScreen.kt
// Shows: Verdict, Scanned URL, Why this result, View Security Analysis, Open in Sandbox, Back

package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weblinkscanner.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    viewModel: ScanViewModel,
    onSandboxClick: (url: String, scanId: String) -> Unit,
    onSecurityAnalysisClick: (url: String, scanId: String) -> Unit,
    onBack: () -> Unit
) {
    val result by viewModel.scanResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan result") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.clearResult(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (result == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No result available.")
            }
            return@Scaffold
        }

        val scan = result!!

        val verdictColor = when (scan.riskLevel.lowercase()) {
            "safe"       -> Color(0xFF2E7D32)
            "suspicious" -> Color(0xFFF57F17)
            "dangerous"  -> Color(0xFFC62828)
            else         -> Color(0xFF546E7A)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Verdict: SAFE / SUSPICIOUS / DANGEROUS
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Verdict: ${scan.riskLevel.uppercase()}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = verdictColor
                )
            }

            // Scanned URL
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Scanned URL: ${scan.url ?: "N/A"}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Why this result (threat reasons)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Why this result:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (scan.threatCategories.isNotEmpty()) {
                        scan.threatCategories.forEach { reason ->
                            Text("• $reason", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("• ${scan.message ?: "No issues detected"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(onClick = { onSecurityAnalysisClick(scan.url ?: "", scan.scanId) }, modifier = Modifier.fillMaxWidth()) {
                Text("View Security Analysis")
            }

            Button(onClick = { onSandboxClick(scan.url ?: "", scan.scanId) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open link in Sandbox")
            }

            OutlinedButton(onClick = { viewModel.clearResult(); onBack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}