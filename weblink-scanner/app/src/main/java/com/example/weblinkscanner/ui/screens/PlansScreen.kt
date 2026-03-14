// WeblinkScanner\app\src\main\java\com\example\weblinkscanner\ui\screens\PlansScreen.kt

package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weblinkscanner.viewmodel.PlanViewModel

@Composable
fun PlansScreen(
    viewModel: PlanViewModel,
    onUpgradeClick: (String) -> Unit,
    onBack: () -> Unit
) {

    val myPlan by viewModel.myPlan.collectAsState()
    val plans by viewModel.allPlans.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyPlan()
        viewModel.loadAllPlans()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text("Subscription Plans", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        }

        if (error != null) {
            Text(error!!)
        }

        myPlan?.let {
            Text("Current Plan: ${it.planDetails.name}")
            Text("Scans Today: ${it.scansToday}")
        }

        Spacer(Modifier.height(20.dp))

        plans.forEach { plan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {

                Column(Modifier.padding(16.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Text(plan.price)
                    Text(plan.scanLimit)

                    Spacer(Modifier.height(8.dp))

                    plan.features.forEach {
                        Text("• $it")
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.upgradePlan(plan.name.lowercase()) }
                    ) {
                        Text("Choose Plan")
                    }

                }
            }
        }

    }
}

