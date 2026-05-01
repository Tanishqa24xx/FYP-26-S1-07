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
import com.example.weblinkscanner.ui.theme.WeblinkScannerTheme
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
import com.example.weblinkscanner.ui.screens.WarningStrictnessScreen
import com.example.weblinkscanner.ui.screens.UserSupportScreen
import com.example.weblinkscanner.ui.screens.BrowseScanScreen
import com.example.weblinkscanner.utils.WarningStrictnessManager
import com.example.weblinkscanner.ui.screens.admin.AdminDashboardScreen
import com.example.weblinkscanner.ui.screens.admin.UserManagementScreen
import com.example.weblinkscanner.ui.screens.admin.UserDetailScreen
import com.example.weblinkscanner.ui.screens.admin.UserProfilesScreen
import com.example.weblinkscanner.ui.screens.admin.UserProfileDetailScreen
import com.example.weblinkscanner.ui.screens.admin.SecurityMonitorScreen
import com.example.weblinkscanner.ui.screens.admin.ScanRecordsScreen
import com.example.weblinkscanner.ui.screens.admin.FlaggedLinksScreen
import com.example.weblinkscanner.ui.screens.admin.AuditLogScreen
import com.example.weblinkscanner.ui.screens.admin.SubscriptionManagementScreen
import com.example.weblinkscanner.viewmodel.AdminViewModel
import com.example.weblinkscanner.viewmodel.PlatformViewModel
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
        val savedName    = TokenManager.getSavedName(this)
        val savedEmail   = TokenManager.getSavedEmail(this)
        val savedPlan    = TokenManager.getSavedPlan(this)
        val savedUserId  = TokenManager.getSavedUserId(this)
        val savedRole    = TokenManager.getSavedRole(this)
        val startDest    = when {
            hasSession && savedRole == "admin"            -> "admin_dashboard"
            hasSession && savedRole == "platform_manager" -> "pm_dashboard"
            hasSession                                    -> "menu"
            else                                          -> "login"
        }
        setContent {
            WeblinkScannerTheme {
                AppNavigation(
                    startDestination = startDest,
                    sessionStore     = sessionStore,
                    savedName        = savedName,
                    savedEmail       = savedEmail,
                    savedPlan        = savedPlan,
                    savedUserId      = savedUserId,
                    savedRole        = savedRole
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
    savedUserId:      String = "00000000-0000-0000-0000-000000000000",
    savedRole:        String = "user"
) {
    val context       = LocalContext.current
    val navController = rememberNavController()

    var loggedInName   by remember { mutableStateOf(savedName) }
    var loggedInEmail  by remember { mutableStateOf(savedEmail) }
    var loggedInPlan   by remember { mutableStateOf(savedPlan) }
    var loggedInUserId by remember { mutableStateOf(savedUserId) }
    var loggedInToken  by remember { mutableStateOf("") }
    var loggedInRole   by remember { mutableStateOf(savedRole) }

    val repository       = remember { WeblinkScannerRepository(sessionStore) }
    val scanViewModel    = remember { ScanViewModel(repository) }
    val sandboxViewModel = remember { SandboxViewModel(repository) }
    val planViewModel    = remember { PlanViewModel(repository) }
    val adminViewModel   = remember { AdminViewModel() }
    val platformViewModel = remember { PlatformViewModel() }

    // Sync loggedInPlan whenever planViewModel refreshes (e.g. after upgrade or screen resume)
    val myPlanState by planViewModel.myPlan.collectAsState()
    LaunchedEffect(myPlanState) {
        val newPlan = myPlanState?.currentPlan
        if (!newPlan.isNullOrBlank()) {
            loggedInPlan = newPlan.lowercase()
        }
    }

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

        // ── Login ──────────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onLoginSuccess = { name, email, plan, userId, token, role ->
                    loggedInName   = name
                    loggedInEmail  = email
                    loggedInPlan   = plan
                    loggedInUserId = userId
                    loggedInToken  = token
                    loggedInRole   = role
                    val dest = when (role) {
                        "admin"            -> "admin_dashboard"
                        "platform_manager" -> "pm_dashboard"
                        else               -> "menu"
                    }
                    navController.navigate(dest) {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        // ── Sign Up ────────────────────────────────────────────────────────────
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess   = {
                    navController.navigate("login") { popUpTo("signup") { inclusive = true } }
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
                onNavigateToScan        = { navController.navigate("scan_url") },
                onNavigateToMyPlan      = { navController.navigate("my_plan") },
                onNavigateToScanHistory = { navController.navigate("scan_history") },
                onNavigateToSavedLinks  = { navController.navigate("saved_links") },
                onNavigateToSettings    = { navController.navigate("settings") },
                onNavigateToBrowseScan  = { navController.navigate("browse_scan") },
                onLogout = {
                    scanViewModel.clearRecentScans()
                    TokenManager.clearSession(context)
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    loggedInUserId = "00000000-0000-0000-0000-000000000000"; loggedInToken = ""
                    navController.navigate("login") { popUpTo("menu") { inclusive = true } }
                }
            )
        }

        // ── Scan URL ───────────────────────────────────────────────────────────
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

        // ── Camera ─────────────────────────────────────────────────────────────
        composable("camera") {
            CameraScreen(
                viewModel      = scanViewModel,
                userId         = loggedInUserId,
                onScanComplete = { navController.navigate("result") },
                onBack         = { navController.popBackStack() }
            )
        }

        // ── QR ─────────────────────────────────────────────────────────────────
        composable("qr") {
            QrScreen(
                viewModel      = scanViewModel,
                userId         = loggedInUserId,
                onScanComplete = { navController.navigate("result") },
                onBack         = { navController.popBackStack() }
            )
        }

        // ── Scan Result ────────────────────────────────────────────────────────
        composable("result") {
            ScanResultScreen(
                viewModel               = scanViewModel,
                repository              = repository,
                userId                  = loggedInUserId,
                userPlan                = loggedInPlan,
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

        // ── Sandbox ────────────────────────────────────────────────────────────
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
                userId = loggedInUserId,
                onBack = { navController.popBackStack() })
        }

        // ── Security Analysis ──────────────────────────────────────────────────
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
                userId           = loggedInUserId,
                verdict          = verdict,
                threatCategories = categories,
                userPlan         = loggedInPlan,
                onBack           = { navController.popBackStack() }
            )
        }

        // ── My Plan ────────────────────────────────────────────────────────────
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

        // ── Plans ──────────────────────────────────────────────────────────────
        composable("plans") {
            LaunchedEffect(Unit) { planViewModel.loadMyPlan(loggedInUserId) }
            PlansScreen(
                viewModel      = planViewModel,
                userId         = loggedInUserId,
                onUpgradeClick = { plan -> navController.navigate("upgrade_plan/$plan") },
                onBack         = { navController.popBackStack() }
            )
        }

        // ── Upgrade Plan ───────────────────────────────────────────────────────
        composable(
            route     = "upgrade_plan/{plan}",
            arguments = listOf(navArgument("plan") { type = NavType.StringType })
        ) { back ->
            val plan = back.arguments?.getString("plan") ?: "standard"
            LaunchedEffect(Unit) { planViewModel.loadMyPlan(loggedInUserId) }
            UpgradePlanScreen(
                viewModel       = planViewModel,
                userId          = loggedInUserId,
                preSelectedPlan = plan,
                onBack          = {
                    planViewModel.loadMyPlan(loggedInUserId)
                    navController.popBackStack()
                }
            )
        }

        // ── Scan History ───────────────────────────────────────────────────────
        composable("scan_history") {
            ScanHistoryScreen(
                repository = repository,
                userId     = loggedInUserId,
                userPlan   = loggedInPlan,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Saved Links ────────────────────────────────────────────────────────
        composable("saved_links") {
            SavedLinksScreen(
                repository    = repository,
                scanViewModel = scanViewModel,
                userId        = loggedInUserId,
                onBack        = { navController.popBackStack() }
            )
        }

        // ── Settings ───────────────────────────────────────────────────────────
        composable("settings") {
            val isRegularUser = loggedInRole == "user"
            SettingsScreen(
                onNavigateToEditProfile      = { navController.navigate("edit_profile") },
                onNavigateToAutoLogout       = { navController.navigate("auto_logout") },
                onNavigateToHelpFaq          = { navController.navigate("help_faq") },
                onNavigateToWarningStrictness = { navController.navigate("warning_strictness") },
                onNavigateToSupport          = { navController.navigate("user_support") },
                showWarningStrictness        = isRegularUser,
                showReportSupport            = isRegularUser,
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

        // ── Help FAQ ───────────────────────────────────────────────────────────
        composable("help_faq") {
            HelpFaqScreen(
                repository = repository,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Auto Logout ────────────────────────────────────────────────────────
        composable("auto_logout") {
            AutoLogoutScreen(userId = loggedInUserId, onBack = { navController.popBackStack() })
        }

        // ── Warning Strictness ─────────────────────────────────────────────────
        composable("warning_strictness") {
            WarningStrictnessScreen(userId = loggedInUserId, onBack = { navController.popBackStack() })
        }

        // ── User Support / Report ──────────────────────────────────────────────
        composable("user_support") {
            UserSupportScreen(
                repository = repository,
                userId     = loggedInUserId,
                userEmail  = loggedInEmail,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Browse & Scan (Standard + Premium) ────────────────────────────────
        composable("browse_scan") {
            BrowseScanScreen(
                repository = repository,
                userId     = loggedInUserId,
                userPlan   = loggedInPlan,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Edit Profile ───────────────────────────────────────────────────────
        composable("edit_profile") {
            EditProfileScreen(
                repository   = repository,
                userId       = loggedInUserId,
                currentName  = loggedInName,
                currentEmail = loggedInEmail,
                onSave       = { newName, newEmail ->
                    loggedInName  = newName
                    loggedInEmail = newEmail
                    navController.popBackStack()
                },
                onCancel     = { navController.popBackStack() }
            )
        }

        // ── Admin Dashboard ────────────────────────────────────────────────────
        composable("admin_dashboard") {
            AdminDashboardScreen(
                adminName  = loggedInName,
                adminEmail = loggedInEmail,
                token      = loggedInToken,
                viewModel  = adminViewModel,
                onNavigateToUserManagement  = { navController.navigate("admin_users") },
                onNavigateToUserProfiles    = { navController.navigate("admin_profiles") },
                onNavigateToSecurityMonitor = { navController.navigate("admin_security") },
                onNavigateToScanRecords     = { navController.navigate("admin_scans") },
                onNavigateToFlaggedLinks    = { navController.navigate("admin_flagged") },
                onNavigateToAuditLog        = { navController.navigate("admin_audit") },
                onNavigateToSubscriptions   = { navController.navigate("admin_subscriptions") },
                onNavigateToSettings        = { navController.navigate("settings") },
                onLogout = {
                    scanViewModel.clearRecentScans()
                    TokenManager.clearSession(context)
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    loggedInUserId = "00000000-0000-0000-0000-000000000000"
                    loggedInToken = ""; loggedInRole = "user"
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── Admin User Management ──────────────────────────────────────────────
        composable("admin_users") {
            UserManagementScreen(
                token     = loggedInToken,
                viewModel = adminViewModel,
                onUserClick = { userId ->
                    navController.navigate("admin_user_detail/$userId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Admin User Detail ──────────────────────────────────────────────────
        composable(
            route     = "admin_user_detail/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            UserDetailScreen(
                userId    = userId,
                token     = loggedInToken,
                viewModel = adminViewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Admin User Profiles ────────────────────────────────────────────────
        composable("admin_profiles") {
            UserProfilesScreen(
                token          = loggedInToken,
                viewModel      = adminViewModel,
                onProfileClick = { profileId ->
                    navController.navigate("admin_profile_detail/$profileId")
                },
                onBack         = { navController.popBackStack() }
            )
        }

        // ── Admin Profile Detail ───────────────────────────────────────────────
        composable(
            route     = "admin_profile_detail/{profileId}",
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { back ->
            val profileId = back.arguments?.getString("profileId") ?: ""
            UserProfileDetailScreen(
                profileId = profileId,
                token     = loggedInToken,
                viewModel = adminViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("admin_security") {
            SecurityMonitorScreen(
                token     = loggedInToken,
                viewModel = adminViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("admin_scans") {
            ScanRecordsScreen(
                token     = loggedInToken,
                viewModel = adminViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("admin_flagged") {
            FlaggedLinksScreen(
                token     = loggedInToken,
                viewModel = adminViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("admin_audit") {
            AuditLogScreen(
                token     = loggedInToken,
                viewModel = adminViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("admin_subscriptions") {
            SubscriptionManagementScreen(
                token       = loggedInToken,
                viewModel   = adminViewModel,
                onUserClick = { userId -> navController.navigate("admin_user_detail/$userId") },
                onBack      = { navController.popBackStack() }
            )
        }

        // ── Platform Manager Dashboard ─────────────────────────────────────────
        composable("pm_dashboard") {
            com.example.weblinkscanner.ui.screens.platform.PMDashboardScreen(
                pmName    = loggedInName,
                pmEmail   = loggedInEmail,
                token     = loggedInToken,
                viewModel = platformViewModel,
                onNavigateToPlans     = { navController.navigate("pm_plans") },
                onNavigateToAnalytics = { navController.navigate("pm_analytics") },
                onNavigateToReports   = { navController.navigate("pm_reports") },
                onNavigateToSupport   = { navController.navigate("pm_support") },
                onNavigateToFaq       = { navController.navigate("pm_faq") },
                onNavigateToHealth    = { navController.navigate("pm_health") },
                onNavigateToSettings  = { navController.navigate("settings") },
                onLogout = {
                    TokenManager.clearSession(context)
                    loggedInName = ""; loggedInEmail = ""; loggedInPlan = "FREE"
                    loggedInUserId = "00000000-0000-0000-0000-000000000000"
                    loggedInToken = ""; loggedInRole = "user"
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable("pm_plans") {
            com.example.weblinkscanner.ui.screens.platform.PMSubscriptionPlansScreen(
                token       = loggedInToken,
                viewModel   = platformViewModel,
                onPlanClick = { planId -> navController.navigate("pm_plan_detail/$planId") },
                onBack      = { navController.popBackStack() }
            )
        }
        composable(
            route     = "pm_plan_detail/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.StringType })
        ) { back ->
            com.example.weblinkscanner.ui.screens.platform.PMPlanDetailScreen(
                planId    = back.arguments?.getString("planId") ?: "",
                token     = loggedInToken,
                viewModel = platformViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("pm_analytics") {
            com.example.weblinkscanner.ui.screens.platform.PMAnalyticsScreen(
                token     = loggedInToken,
                viewModel = platformViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("pm_reports") {
            com.example.weblinkscanner.ui.screens.platform.PMReportsScreen(
                token     = loggedInToken,
                viewModel = platformViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("pm_support") {
            com.example.weblinkscanner.ui.screens.platform.PMSupportScreen(
                token           = loggedInToken,
                viewModel       = platformViewModel,
                onRequestClick  = { requestId -> navController.navigate("pm_support_detail/$requestId") },
                onBack          = { navController.popBackStack() }
            )
        }
        composable(
            route     = "pm_support_detail/{requestId}",
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { back ->
            com.example.weblinkscanner.ui.screens.platform.PMSupportDetailScreen(
                requestId = back.arguments?.getString("requestId") ?: "",
                token     = loggedInToken,
                viewModel = platformViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("pm_faq") {
            com.example.weblinkscanner.ui.screens.platform.PMFaqScreen(
                token     = loggedInToken,
                viewModel = platformViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("pm_health") {
            com.example.weblinkscanner.ui.screens.platform.PMSystemHealthScreen(
                token     = loggedInToken,
                viewModel = platformViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
