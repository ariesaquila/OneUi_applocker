package com.oneui.applocker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oneui.applocker.core.security.SecurityManager
import com.oneui.applocker.ui.home.HomeScreen
import com.oneui.applocker.ui.home.HomeViewModel
import com.oneui.applocker.ui.permissions.PermissionsScreen
import com.oneui.applocker.ui.permissions.PermissionsViewModel
import com.oneui.applocker.ui.settings.SettingsScreen
import com.oneui.applocker.ui.settings.SettingsViewModel
import com.oneui.applocker.ui.setup.SetupMasterKeyScreen

import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.oneui.applocker.data.model.LockType

@Composable
fun AppNavHost(
    securityManager: SecurityManager,
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = Screen.SetupCredentials.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "PIN"
                }
            )
        ) { backStackEntry ->
            val typeArg = backStackEntry.arguments?.getString("type")
            val initialType = if (typeArg == "PATTERN") LockType.PATTERN else LockType.PIN
            SetupMasterKeyScreen(
                securityManager = securityManager,
                initialLockType = initialType,
                isChangeMode = securityManager.isConfigured(),
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null,
                onSetupComplete = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Permissions.route) {
                            popUpTo(Screen.SetupCredentials.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            val permissionsViewModel: PermissionsViewModel = viewModel()
            PermissionsScreen(
                viewModel = permissionsViewModel,
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                },
                onNavigateToSetupCredentials = { lockType ->
                    val typeArg = if (lockType == LockType.PATTERN) "PATTERN" else "PIN"
                    navController.navigate(Screen.SetupCredentials.createRoute(typeArg))
                }
            )
        }
    }
}
