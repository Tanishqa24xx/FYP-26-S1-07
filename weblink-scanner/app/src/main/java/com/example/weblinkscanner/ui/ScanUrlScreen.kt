// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\ScanUrlScreen.kt

package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weblinkscanner.viewmodel.PlanViewModel
import com.example.weblinkscanner.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanUrlScreen(
    scanViewModel: ScanViewModel,
    planViewModel: PlanViewModel,
    onScanComplete: () -> Unit,
    onCameraClick: () -> Unit,
    onQrClick: () -> Unit,
    onBack: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }

    val isLoading    by scanViewModel.isLoading.collectAsState()
    val errorMessage by scanViewModel.errorMessage.collectAsState()
    val scanResult   by scanViewModel.scanResult.collectAsState()

    val myPlan by planViewModel.myPlan.collectAsState()

    LaunchedEffect(Unit) { planViewModel.loadMyPlan() }

    // Navigate when scan completes
    LaunchedEffect(scanResult) {
        if (scanResult != null) onScanComplete()
    }

    // Plan quota values shown in the info bar
    val planName   = myPlan?.currentPlan?.uppercase() ?: "FREE"
    val dailyLimit = myPlan?.dailyLimit ?: 5
    val scansToday = myPlan?.scansToday ?: 0
    val remaining  = (dailyLimit - scansToday).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan URL") },
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

            // Plan / quota info bar
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$planName Plan • $dailyLimit scans/day • Remaining: $remaining",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // URL input
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Paste URL here (https://...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Error message
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Scan button
            Button(
                onClick = { scanViewModel.scanUrl(urlInput.trim()) },
                enabled = urlInput.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Scan")
                }
            }

            // Camera and QR buttons side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCameraClick, modifier = Modifier.weight(1f)) {
                    Text("Capture via Camera")
                }
                OutlinedButton(onClick = onQrClick, modifier = Modifier.weight(1f)) {
                    Text("Scan QR")
                }
            }

            // Most recent scan info card
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Recent 5 scans",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.weight(1f))

            // Back button pinned at the bottom
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}