// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\SandboxScreen.kt
// Shows: URL bar, large content area (sandbox report text), note, Back

package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
import com.example.weblinkscanner.viewmodel.SandboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel,
    url: String,
    scanId: String,
    onBack: () -> Unit
) {
    val report  by viewModel.sandboxReport.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error   by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.analyseSandbox(url, scanId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sandbox Environment") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // URL bar
            OutlinedTextField(
                value = url,
                onValueChange = {},
                readOnly = true,
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Content area — shows loading, error, or sandbox report
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {

                if (loading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Running sandbox analysis…", style = MaterialTheme.typography.bodySmall)
                    }

                } else if (error != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.analyseSandbox(url, scanId) }) {
                            Text("Retry")
                        }
                    }

                } else if (report != null) {
                    val r = report!!
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text("Page title: ${r.pageTitle ?: "N/A"}")

                        if (r.redirectChain.isNotEmpty()) {
                            Text("Redirects:")
                            r.redirectChain.forEach {
                                Text("  → $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (r.externalLinks.isNotEmpty()) {
                            Text("External links found: ${r.externalLinks.size}")
                        }

                        /* placing screenshot
                        if (!r.screenshotPath.isNullOrBlank()) {

                            Spacer(Modifier.height(12.dp))

                            AsyncImage(
                                model = r.screenshotPath,
                                contentDescription = "Website Screenshot",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }*/
                    }

                } else {

                    Text("Contents of the Website Displayed Here.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                "Note: Sandbox analysis may take longer on slow connections.",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}