// app/kotlin+java/com.example.weblinkscanner/ui/navigation/NavGraph.kt
// Navigation graph for the entire app.
// Routes owned by this developer: PLAN, SCAN_URL, CAMERA, QR, RESULT, SANDBOX

package com.example.weblinkscanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.weblinkscanner.data.repository.SessionStore
import com.example.weblinkscanner.data.repository.WeblinkScannerRepository
import com.example.weblinkscanner.ui.screens.*
import com.example.weblinkscanner.viewmodel.*

object Routes {
    const val SCAN_URL        = "scan_url"
    const val CAMERA          = "camera"
    const val QR              = "qr"
    const val RESULT          = "result"
    const val SANDBOX         = "sandbox/{url}/{scanId}"
    const val SECURITY        = "security/{url}/{scanId}"
    const val MY_PLAN         = "my_plan"
    const val PLANS           = "plans"
    const val UPGRADE_PLAN    = "upgrade_plan/{plan}"

    // URL-encodes the url segment to handle slashes and special chars
    fun sandboxRoute(url: String, scanId: String): String {
        val encoded = java.net.URLEncoder.encode(url, "UTF-8")
        return "sandbox/$encoded/$scanId"
    }

    fun securityRoute(url: String, scanId: String): String {
        val encoded = java.net.URLEncoder.encode(url, "UTF-8")
        return "security/$encoded/$scanId"
    }

    fun upgradePlanRoute(plan: String) = "upgrade_plan/$plan"
}

@Composable
fun NavGraph(sessionStore: SessionStore) {
    val navController = rememberNavController()
    val repository    = remember { WeblinkScannerRepository(sessionStore) }

    val scanViewModel    = remember { ScanViewModel(repository) }
    val sandboxViewModel = remember { SandboxViewModel(repository) }
    val planViewModel    = remember { PlanViewModel(repository) }

    NavHost(navController = navController, startDestination = Routes.SCAN_URL) {

        // ----- Manual URL Scan -----
        composable(Routes.SCAN_URL) {
            ScanUrlScreen(
                scanViewModel  = scanViewModel,
                planViewModel  = planViewModel,
                onScanComplete = { navController.navigate(Routes.RESULT) },
                onCameraClick  = { navController.navigate(Routes.CAMERA) },
                onQrClick      = { navController.navigate(Routes.QR) },
                onBack         = { navController.popBackStack() },
            )
        }

        // ----- Camera Scan -----
        composable(Routes.CAMERA) {
            CameraScreen(
                viewModel      = scanViewModel,
                onScanComplete = { navController.navigate(Routes.RESULT) },
                onBack         = { navController.popBackStack() },
            )
        }

        // ----- QR Scan -----
        composable(Routes.QR) {
            QrScreen(
                viewModel      = scanViewModel,
                onScanComplete = { navController.navigate(Routes.RESULT) },
                onBack         = { navController.popBackStack() },
            )
        }

        // ----- Scan Result -----
        composable(Routes.RESULT) {
            ScanResultScreen(
                viewModel                = scanViewModel,
                onSandboxClick           = { url, scanId ->
                    navController.navigate(Routes.sandboxRoute(url, scanId))
                },
                onSecurityAnalysisClick  = { url, scanId ->
                    navController.navigate(Routes.securityRoute(url, scanId))
                },
                onBack                   = { navController.popBackStack() },
            )
        }

        // ----- Sandbox -----
        composable(
            route = Routes.SANDBOX,
            arguments = listOf(
                navArgument("url")    { type = NavType.StringType },
                navArgument("scanId") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val url    = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
            SandboxScreen(
                viewModel = sandboxViewModel,
                url       = url,
                scanId    = scanId,
                onBack    = { navController.popBackStack() },
            )
        }

        // ----- Security Analysis -----
        composable(
            route = Routes.SECURITY,
            arguments = listOf(
                navArgument("url")    { type = NavType.StringType },
                navArgument("scanId") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val url    = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
            SecurityAnalysisScreen(
                viewModel = sandboxViewModel,
                url       = url,
                scanId    = scanId,
                onBack    = { navController.popBackStack() },
            )
        }

        // ----- My Plan -----
        composable(Routes.MY_PLAN) {
            MyPlanScreen(
                viewModel           = planViewModel,
                onViewPaidPlansClick = { navController.navigate(Routes.PLANS) },
                onBack              = { navController.popBackStack() },
            )
        }

        // ----- Plans (all plans list) -----
        composable(Routes.PLANS) {
            PlansScreen(
                viewModel       = planViewModel,
                onUpgradeClick  = { plan -> navController.navigate(Routes.upgradePlanRoute(plan)) },
                onBack          = { navController.popBackStack() },
            )
        }

        // ----- Upgrade Plan -----
        composable(
            route = Routes.UPGRADE_PLAN,
            arguments = listOf(navArgument("plan") { type = NavType.StringType })
        ) { backStackEntry ->
            val plan = backStackEntry.arguments?.getString("plan") ?: "standard"
            UpgradePlanScreen(
                viewModel       = planViewModel,
                preSelectedPlan = plan,
                onBack          = { navController.popBackStack() },
            )
        }

    }
}
