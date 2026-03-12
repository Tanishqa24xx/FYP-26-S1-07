package com.example.linkscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.linkscanner.ui.theme.LinkScannerTheme
import com.example.linkscanner.ui.login.LoginScreen
import com.example.linkscanner.ui.theme.signup.SignUpScreen
import com.example.linkscanner.ui.scanner.ScannerScreen
import com.example.linkscanner.ui.menu.MenuScreen
import com.example.linkscanner.ui.settings.SettingsScreen
import com.example.linkscanner.ui.settings.EditProfileScreen
import com.example.linkscanner.utils.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hasSession = TokenManager.hasValidSession(this)
        setContent {
            LinkScannerTheme {
                AppNavigation(startDestination = if (hasSession) "menu" else "login")
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String = "login") {
    val navController = rememberNavController()

    var loggedInName  by remember { mutableStateOf("") }
    var loggedInEmail by remember { mutableStateOf("") }
    var loggedInPlan  by remember { mutableStateOf("FREE") }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Login ──────────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onLoginSuccess = { name, email, plan ->
                    loggedInName  = name
                    loggedInEmail = email
                    loggedInPlan  = plan
                    navController.navigate("menu") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        // ── Sign Up ────────────────────────────────────────────────────────────
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Menu ───────────────────────────────────────────────────────────────
        composable("menu") {
            MenuScreen(
                userName                = loggedInName,
                userEmail               = loggedInEmail,
                userPlan                = loggedInPlan,
                onNavigateToScan        = { navController.navigate("scanner") },
                onNavigateToMyPlan      = { /* TODO */ },
                onNavigateToScanHistory = { /* TODO */ },
                onNavigateToSavedLinks  = { /* TODO */ },
                onNavigateToSettings    = { navController.navigate("settings") },
                onLogout = {
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    navController.navigate("login") { popUpTo("menu") { inclusive = true } }
                }
            )
        }

        // ── Scanner ────────────────────────────────────────────────────────────
        composable("scanner") {
            ScannerScreen(
                userPlan       = loggedInPlan,
                scansRemaining = 5,
                onBack         = { navController.popBackStack() },
                onLogout       = {
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── Settings ───────────────────────────────────────────────────────────
        composable("settings") {
            SettingsScreen(
                onNavigateToEditProfile = { navController.navigate("edit_profile") },
                onNavigateToAutoLogout  = { /* TODO */ },
                onNavigateToHelpFaq     = { /* TODO */ },
                onDeleteAccount         = {
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Edit Profile ───────────────────────────────────────────────────────
        composable("edit_profile") {
            EditProfileScreen(
                currentName  = loggedInName,
                currentEmail = loggedInEmail,
                onSave       = { navController.popBackStack() },
                onCancel     = { navController.popBackStack() }
            )
        }
    }
}