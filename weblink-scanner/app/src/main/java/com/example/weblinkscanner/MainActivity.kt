package com.example.weblinkscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.weblinkscanner.data.repository.SessionStore
import com.example.weblinkscanner.ui.navigation.NavGraph
import com.example.weblinkscanner.ui.theme.WeblinkScannerTheme

class MainActivity : ComponentActivity() {
    private lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize session store
        sessionStore = SessionStore(applicationContext)

        setContent {
            WeblinkScannerTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    // Pass sessionStore so the Repository (and all ViewModels)
                    // can read the saved token without needing a Context.
                    NavGraph(sessionStore = sessionStore)
                }
            }
        }
    }
}
