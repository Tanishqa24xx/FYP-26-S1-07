// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\MyPlanScreen.kt
// Shows: current plan, status/billing, benefits, limits, View paid plans button, Back

package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weblinkscanner.viewmodel.PlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlanScreen(
    viewModel: PlanViewModel,
    onViewPaidPlansClick: () -> Unit,
    onBack: () -> Unit
) {
    val myPlan by viewModel.myPlan.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMyPlan() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My plan") },
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

            if (loading) { CircularProgressIndicator() }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            val planName = myPlan?.currentPlan?.uppercase() ?: "FREE"
            val dailyLimit = myPlan?.dailyLimit ?: 5

            // Current plan
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current plan: $planName",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Status and billing
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Status: Active")
                    Text("Billing: ${if (planName == "FREE") "None (Free plan)" else "Monthly"}")
                }
            }

            // Benefits
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your benefits:", fontWeight = FontWeight.SemiBold)
                    Text("• Scan URLs (paste / camera / QR)")
                    Text("• Risk level: Safe / Suspicious / Dangerous")
                    Text("• Scan history: last 5 (if enabled)")
                    Text("• Daily scan limit applies")
                    Text("• Sandbox Environment available")
                }
            }

            // Limits
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your limits:", fontWeight = FontWeight.SemiBold)
                    Text("• $dailyLimit scans/day")
                    Text("• History stores 5 most recent scans")
                    Text("• Basic analysis only")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(onClick = onViewPaidPlansClick, modifier = Modifier.fillMaxWidth()) {
                Text("View paid plans")
            }

            Text(
                "Upgrading increases scan limits and unlocks more analysis.",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
