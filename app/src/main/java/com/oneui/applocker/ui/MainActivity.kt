package com.oneui.applocker.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.oneui.applocker.AppLockerApp
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.core.theme.OneUiAppLockerTheme
import com.oneui.applocker.data.model.LockSettings
import com.oneui.applocker.data.model.ThemeMode
import com.oneui.applocker.service.AppMonitorForegroundService
import com.oneui.applocker.ui.navigation.AppNavHost
import com.oneui.applocker.ui.navigation.Screen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide app contents from Recent Apps / Task Switcher preview and prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val app = application as AppLockerApp
        val securityManager = app.securityManager
        val settingsRepository = app.settingsRepository

        // Determine start destination:
        // 1. If security PIN/Pattern not configured -> SetupCredentials
        // 2. If essential permissions missing -> Permissions
        // 3. Otherwise -> Home
        val startDestination = when {
            !securityManager.isConfigured() -> Screen.SetupCredentials.route
            !PermissionHelper.hasAllMandatoryPermissions(this) -> Screen.Permissions.route
            else -> {
                manageBackgroundService()
                Screen.Home.route
            }
        }

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState(initial = LockSettings())
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            OneUiAppLockerTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        securityManager = securityManager,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        manageBackgroundService()
    }

    /**
     * If Accessibility Service is enabled, it handles 100% of protection with 0ms latency.
     * In that case, we STOP AppMonitorForegroundService so NO notification appears in the notification shade!
     */
    private fun manageBackgroundService() {
        if (PermissionHelper.isAccessibilityServiceEnabled(this)) {
            AppMonitorForegroundService.stop(this)
        } else if (PermissionHelper.hasUsageAccessPermission(this)) {
            // Only run as fallback when accessibility service is not active
            AppMonitorForegroundService.start(this)
        }
    }
}
