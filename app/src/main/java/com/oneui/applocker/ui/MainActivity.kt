package com.oneui.applocker.ui

import android.content.Intent
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
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.core.theme.OneUiAppLockerTheme
import com.oneui.applocker.core.util.DisplayRefreshRateHelper
import com.oneui.applocker.data.model.LockSettings
import com.oneui.applocker.data.model.ThemeMode
import com.oneui.applocker.service.AppMonitorForegroundService
import com.oneui.applocker.ui.lock.LockActivity
import com.oneui.applocker.ui.navigation.AppNavHost
import com.oneui.applocker.ui.navigation.Screen

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = com.oneui.applocker.core.util.LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(com.oneui.applocker.core.util.LocaleHelper.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync display refresh rate to device hardware (90Hz / 120Hz / 144Hz)
        DisplayRefreshRateHelper.syncWithDeviceRefreshRate(this)

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

            OneUiAppLockerTheme(themeMode = settings.themeMode) {
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
        if (checkAppLock()) {
            return
        }
        AppLockStateHolder.activeUnlockedPackage = packageName
        AppLockStateHolder.hasEnteredTargetApp = true
        AppLockStateHolder.lastForegroundPackage = packageName
    }

    override fun onStop() {
        super.onStop()
        if (AppLockStateHolder.isPackageLocked(packageName)) {
            if (AppLockStateHolder.relockPolicy == com.oneui.applocker.core.security.RelockPolicy.IMMEDIATELY) {
                AppLockStateHolder.clearSessionForPackage(packageName)
            }
        }
    }

    private fun checkAppLock(): Boolean {
        val app = application as AppLockerApp
        val securityManager = app.securityManager
        if (securityManager.isConfigured() &&
            PermissionHelper.hasAllMandatoryPermissions(this) &&
            AppLockStateHolder.shouldIntercept(packageName)
        ) {
            val intent = LockActivity.newIntent(this, packageName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
            return true
        }
        return false
    }

    private fun manageBackgroundService() {
        if (PermissionHelper.hasUsageAccessPermission(this)) {
            AppMonitorForegroundService.start(this)
        }
    }
}
