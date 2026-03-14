// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\UpgradePlanScreen.kt

// Shows: current plan, radio buttons (Standard/Premium), benefits, payment method placeholder, summary, Confirm, Cancel

package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weblinkscanner.viewmodel.PlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradePlanScreen(
    viewModel: PlanViewModel,
    preSelectedPlan: String = "standard",   // passed from PlansScreen
    onBack: () -> Unit
) {
    val myPlan  by viewModel.myPlan.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error   by viewModel.errorMessage.collectAsState()

    var selectedPlan by remember { mutableStateOf(preSelectedPlan) }

    val currentPlan = myPlan?.currentPlan?.uppercase() ?: "FREE"
    val price = if (selectedPlan == "standard") "$4.99" else "$9.99"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade plan") },
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

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            // Current plan
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current plan: $currentPlan",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Plan selector
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select new plan:", fontWeight = FontWeight.SemiBold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedPlan == "standard", onClick = { selectedPlan = "standard" })
                        Text("Standard")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedPlan == "premium", onClick = { selectedPlan = "premium" })
                        Text("Premium")
                    }
                }
            }

            // What you'll get
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("What you'll get:", fontWeight = FontWeight.SemiBold)
                    Text("• Higher scan limits")
                    Text("• Longer scan history")
                    Text("• More security checks")
                }
            }

            // Payment method placeholder
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("Payment method", modifier = Modifier.padding(16.dp))
            }

            // Summary
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Summary:", fontWeight = FontWeight.SemiBold)
                    Text("Plan: ${selectedPlan.replaceFirstChar { it.uppercase() }}")
                    Text("Price: $price / month")
                    Text("Starts: Today")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.upgradePlan(selectedPlan); onBack() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm upgrade", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }

            Text(
                "You can downgrade or cancel anytime in Plan settings.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
