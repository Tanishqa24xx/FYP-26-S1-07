package com.example.weblinkscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.weblinkscanner.data.repository.SessionStore
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import com.example.weblinkscanner.ui.theme.LinkScannerTheme
import com.example.weblinkscanner.ui.screens.LoginScreen
import com.example.weblinkscanner.ui.screens.SignUpScreen
import com.example.weblinkscanner.ui.settings.SettingsScreen
import com.example.weblinkscanner.ui.screens.EditProfileScreen
import com.example.weblinkscanner.ui.menu.MenuScreen
import com.example.weblinkscanner.ui.screens.ScanUrlScreen
import com.example.weblinkscanner.ui.screens.CameraScreen
import com.example.weblinkscanner.ui.screens.QrScreen
import com.example.weblinkscanner.ui.screens.ScanResultScreen
import com.example.weblinkscanner.ui.screens.SandboxScreen
import com.example.weblinkscanner.ui.screens.SecurityAnalysisScreen
import com.example.weblinkscanner.ui.screens.MyPlanScreen
import com.example.weblinkscanner.ui.screens.PlansScreen
import com.example.weblinkscanner.ui.screens.UpgradePlanScreen
import com.example.weblinkscanner.ui.screens.ScanHistoryScreen
import com.example.weblinkscanner.ui.screens.SavedLinksScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.example.weblinkscanner.ui.screens.HelpFaqScreen
import com.example.weblinkscanner.ui.screens.AutoLogoutScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.weblinkscanner.utils.AutoLogoutManager
import com.example.weblinkscanner.utils.TokenManager
import com.example.weblinkscanner.viewmodel.ScanViewModel
import com.example.weblinkscanner.viewmodel.SandboxViewModel
import com.example.weblinkscanner.viewmodel.PlanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hasSession   = TokenManager.hasValidSession(this)
        val sessionStore = SessionStore(applicationContext)
        // Restore saved session info so menu shows correct name/plan on resume
        val savedName    = TokenManager.getSavedName(this)
        val savedEmail   = TokenManager.getSavedEmail(this)
        val savedPlan    = TokenManager.getSavedPlan(this)
        val savedUserId  = TokenManager.getSavedUserId(this)
        setContent {
            LinkScannerTheme {
                AppNavigation(
                    startDestination = if (hasSession) "menu" else "login",
                    sessionStore     = sessionStore,
                    savedName        = savedName,
                    savedEmail       = savedEmail,
                    savedPlan        = savedPlan,
                    savedUserId      = savedUserId
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    startDestination: String = "login",
    sessionStore:     SessionStore,
    savedName:        String = "",
    savedEmail:       String = "",
    savedPlan:        String = "FREE",
    savedUserId:      String = "00000000-0000-0000-0000-000000000000"
) {
    val context       = LocalContext.current
    val navController = rememberNavController()

    // Initialise with saved values so name/plan show immediately on resume
    var loggedInName   by remember { mutableStateOf(savedName) }
    var loggedInEmail  by remember { mutableStateOf(savedEmail) }
    var loggedInPlan   by remember { mutableStateOf(savedPlan) }
    var loggedInUserId by remember { mutableStateOf(savedUserId) }
    var loggedInToken  by remember { mutableStateOf("") }

    val repository       = remember { WeblinkScannerRepository(sessionStore) }
    val scanViewModel    = remember { ScanViewModel(repository) }
    val sandboxViewModel = remember { SandboxViewModel(repository) }
    val planViewModel    = remember { PlanViewModel(repository) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var lastActiveTime by remember { mutableStateOf(System.currentTimeMillis()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val timeoutMs = AutoLogoutManager.getTimeoutMs(navController.context, loggedInUserId)
                val elapsed   = System.currentTimeMillis() - lastActiveTime
                if (elapsed > timeoutMs && loggedInUserId != "00000000-0000-0000-0000-000000000000") {
                    scanViewModel.clearRecentScans()
                    TokenManager.clearSession(context)
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    loggedInUserId = "00000000-0000-0000-0000-000000000000"; loggedInToken = ""
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
                lastActiveTime = System.currentTimeMillis()
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                lastActiveTime = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // --- Login ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = { name, email, plan, userId, token ->
                    loggedInName   = name
                    loggedInEmail  = email
                    loggedInPlan   = plan
                    loggedInUserId = userId
                    loggedInToken  = token
                    navController.navigate("menu") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        // --- Sign Up ---
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess   = {
                    navController.navigate("login") { popUpTo("signup") { inclusive = true } }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // --- Menu ---
        composable("menu") {
            MenuScreen(
                userName                = loggedInName,
                userEmail               = loggedInEmail,
                userPlan                = loggedInPlan,
                onNavigateToScan        = { navController.navigate("scan_url") },
                onNavigateToMyPlan      = { navController.navigate("my_plan") },
                onNavigateToScanHistory = { navController.navigate("scan_history") },
                onNavigateToSavedLinks  = { navController.navigate("saved_links") },
                onNavigateToSettings    = { navController.navigate("settings") },
                onLogout = {
                    scanViewModel.clearRecentScans()
                    scanViewModel.clearRecentScans()
                    TokenManager.clearSession(context)
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    loggedInUserId = "00000000-0000-0000-0000-000000000000"; loggedInToken = ""
                    navController.navigate("login") { popUpTo("menu") { inclusive = true } }
                }
            )
        }

        // --- Scan URL ---
        composable("scan_url") {
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            LaunchedEffect(lifecycleState) {
                if (lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED) {
                    planViewModel.loadMyPlan(loggedInUserId)
                }
            }
            ScanUrlScreen(
                scanViewModel  = scanViewModel,
                planViewModel  = planViewModel,
                userId         = loggedInUserId,
                onScanComplete = { navController.navigate("result") },
                onCameraClick  = { navController.navigate("camera") },
                onQrClick      = { navController.navigate("qr") },
                onBack         = { navController.popBackStack() }
            )
        }

        // --- Camera ---
        composable("camera") {
            CameraScreen(
                viewModel      = scanViewModel,
                userId         = loggedInUserId,
                onScanComplete = { navController.navigate("result") },
                onBack         = { navController.popBackStack() }
            )
        }

        // --- QR ---
        composable("qr") {
            QrScreen(
                viewModel      = scanViewModel,
                userId         = loggedInUserId,
                onScanComplete = { navController.navigate("result") },
                onBack         = { navController.popBackStack() }
            )
        }

        // --- Scan Result ---
        composable("result") {
            ScanResultScreen(
                viewModel               = scanViewModel,
                repository              = repository,
                userId                  = loggedInUserId,
                onSandboxClick          = { url, scanId ->
                    val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                    navController.navigate("sandbox/$encoded/$scanId")
                },
                onSecurityAnalysisClick = { url, scanId, verdict, categories ->
                    val encoded    = java.net.URLEncoder.encode(url, "UTF-8")
                    val catEncoded = java.net.URLEncoder.encode(categories, "UTF-8")
                    navController.navigate("security/$encoded/$scanId?verdict=$verdict&categories=$catEncoded")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // --- Sandbox ---
        composable(
            route     = "sandbox/{url}/{scanId}",
            arguments = listOf(
                navArgument("url")    { type = NavType.StringType },
                navArgument("scanId") { type = NavType.StringType }
            )
        ) { back ->
            val url    = java.net.URLDecoder.decode(back.arguments?.getString("url") ?: "", "UTF-8")
            val scanId = back.arguments?.getString("scanId") ?: ""
            SandboxScreen(viewModel = sandboxViewModel, url = url, scanId = scanId,
                onBack = { navController.popBackStack() })
        }

        // --- Security Analysis ---
        composable(
            route = "security/{url}/{scanId}?verdict={verdict}&categories={categories}",
            arguments = listOf(
                navArgument("url")        { type = NavType.StringType },
                navArgument("scanId")     { type = NavType.StringType },
                navArgument("verdict")    { type = NavType.StringType; defaultValue = "" },
                navArgument("categories") { type = NavType.StringType; defaultValue = "" }
            )
        ) { back ->
            val url        = java.net.URLDecoder.decode(back.arguments?.getString("url") ?: "", "UTF-8")
            val scanId     = back.arguments?.getString("scanId") ?: ""
            val verdict    = back.arguments?.getString("verdict") ?: ""
            val categories = java.net.URLDecoder.decode(back.arguments?.getString("categories") ?: "", "UTF-8")
            SecurityAnalysisScreen(
                viewModel        = sandboxViewModel,
                url              = url,
                scanId           = scanId,
                verdict          = verdict,
                threatCategories = categories,
                onBack           = { navController.popBackStack() }
            )
        }

        // --- My Plan ---
        composable("my_plan") {
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            LaunchedEffect(lifecycleState) {
                if (lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED) {
                    planViewModel.loadMyPlan(loggedInUserId)
                }
            }
            MyPlanScreen(
                viewModel            = planViewModel,
                userId               = loggedInUserId,
                onViewPaidPlansClick = { navController.navigate("plans") },
                onBack               = { navController.popBackStack() }
            )
        }

        // --- Plans ---
        composable("plans") {
            PlansScreen(
                viewModel      = planViewModel,
                onUpgradeClick = { plan -> navController.navigate("upgrade_plan/$plan") },
                onBack         = { navController.popBackStack() }
            )
        }

        // --- Upgrade Plan ---
        composable(
            route     = "upgrade_plan/{plan}",
            arguments = listOf(navArgument("plan") { type = NavType.StringType })
        ) { back ->
            val plan = back.arguments?.getString("plan") ?: "standard"
            UpgradePlanScreen(viewModel = planViewModel, preSelectedPlan = plan,
                onBack = { navController.popBackStack() })
        }

        // --- Scan History ---
        composable("scan_history") {
            ScanHistoryScreen(
                repository = repository,
                userId     = loggedInUserId,
                onBack     = { navController.popBackStack() }
            )
        }

        // --- Saved Links ---
        composable("saved_links") {
            SavedLinksScreen(
                repository    = repository,
                scanViewModel = scanViewModel,
                userId        = loggedInUserId,
                onBack        = { navController.popBackStack() }
            )
        }

        // --- Settings ---
        composable("settings") {
            SettingsScreen(
                onNavigateToEditProfile = { navController.navigate("edit_profile") },
                onNavigateToAutoLogout  = { navController.navigate("auto_logout") },
                onNavigateToHelpFaq     = { navController.navigate("help_faq") },
                onDeleteAccount = {
                    val userIdToDelete = loggedInUserId
                    android.util.Log.d("DELETE", "Starting delete for userId=$userIdToDelete")
                    MainScope().launch {
                        android.util.Log.d("DELETE", "Inside coroutine")
                        try {
                            android.util.Log.d("DELETE", "Calling repository.deleteAccount")
                            val result = repository.deleteAccount(userIdToDelete)
                            android.util.Log.d("DELETE", "Result: $result")
                        } catch (e: Exception) {
                            android.util.Log.e("DELETE", "Exception: ${e.message}")
                        }
                        scanViewModel.clearRecentScans()
                        TokenManager.clearSession(context)
                        loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                        loggedInUserId = "00000000-0000-0000-0000-000000000000"; loggedInToken = ""
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // --- Help FAQ ---
        composable("help_faq") {
            HelpFaqScreen(
                repository = repository,
                onBack     = { navController.popBackStack() }
            )
        }

        // --- Auto Logout ---
        composable("auto_logout") {
            AutoLogoutScreen(userId = loggedInUserId, onBack = { navController.popBackStack() })
        }

        // --- Edit Profile ---
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