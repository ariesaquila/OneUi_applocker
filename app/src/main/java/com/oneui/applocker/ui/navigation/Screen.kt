package com.oneui.applocker.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Permissions : Screen("permissions")
    data object Settings : Screen("settings")
    data object SetupCredentials : Screen("setup_credentials")
}
