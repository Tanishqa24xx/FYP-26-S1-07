package com.example.weblinkscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onScanUrl: () -> Unit = {},
    onScanQr: () -> Unit = {},
    onCameraScan: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "WebLink Scanner",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onScanUrl,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan URL")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onScanQr,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR Code")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCameraScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Camera Scan")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    HomeScreen()
}
