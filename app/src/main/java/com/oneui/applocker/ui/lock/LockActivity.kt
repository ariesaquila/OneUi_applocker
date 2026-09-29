package com.oneui.applocker.ui.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.core.security.BiometricHelper
import com.oneui.applocker.core.theme.OneUiAppLockerTheme
import com.oneui.applocker.core.util.DisplayRefreshRateHelper
import com.oneui.applocker.core.util.LocaleHelper
import com.oneui.applocker.data.model.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LockActivity : FragmentActivity() {

    private lateinit var viewModel: LockViewModel
    private lateinit var biometricHelper: BiometricHelper
    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLockStateHolder.isLockActivityInForeground = true
        overridePendingTransition(0, 0)

        // Sync display refresh rate to device hardware (90Hz / 120Hz / 144Hz) for zero-latency unlocking
        DisplayRefreshRateHelper.syncWithDeviceRefreshRate(this)

        // Protect screen content from task switchers & screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }
        val targetClass = intent.getStringExtra(EXTRA_CLASS_NAME) ?: ""

        // If this app is already unlocked and active, do not display lock screen
        if (!AppLockStateHolder.shouldIntercept(targetPackage)) {
            finish()
            return
        }

        biometricHelper = BiometricHelper(this)

        initViewModel(targetPackage, targetClass)

        // Handle Back button: Go to Home screen so user cannot bypass lock
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                if (targetPackage == packageName) {
                    finishAffinity()
                } else {
                    finish()
                }
            }
        })

        // Collect events (Unlock & Biometrics)
        lifecycleScope.launch {
            viewModel.eventFlow.collectLatest { event ->
                when (event) {
                    is LockUiEvent.UnlockSuccess -> {
                        proceedToTargetApp()
                    }
                    is LockUiEvent.TriggerBiometric -> {
                        triggerBiometric()
                    }
                }
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val currentLocale = remember(uiState.appLanguage) {
                LocaleHelper.getLocaleForLanguage(uiState.appLanguage)
            }
            val configuration = LocalConfiguration.current
            val localizedConfiguration = remember(configuration, currentLocale) {
                Configuration(configuration).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
            }
            val baseContext = LocalContext.current
            val localizedContext = remember(baseContext, currentLocale) {
                LocaleHelper.wrapContext(baseContext, uiState.appLanguage)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalContext provides localizedContext
            ) {
                OneUiAppLockerTheme(themeMode = uiState.themeMode) {
                    LockScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newTarget = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val newClass = intent.getStringExtra(EXTRA_CLASS_NAME) ?: ""
        if (!newTarget.isNullOrBlank() && (newTarget != targetPackage || newClass.isNotEmpty())) {
            targetPackage = newTarget
            initViewModel(newTarget, newClass)
        }
    }

    private fun initViewModel(pkg: String, className: String = "") {
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LockViewModel(application, pkg, className) as T
                }
            }
        )[LockViewModel::class.java]
    }

    private fun proceedToTargetApp() {
        if (targetPackage == packageName && isTaskRoot) {
            val mainIntent = Intent(this, com.oneui.applocker.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(mainIntent)
        }
        finish()
        overridePendingTransition(0, 0)
    }

    private fun triggerBiometric() {
        if (!biometricHelper.canAuthenticate()) return

        biometricHelper.showBiometricPrompt(
            activity = this,
            onSuccess = {
                viewModel.onBiometricSuccess()
            },
            onError = { _, _ ->
                // Biometric error, user can enter PIN/Pattern fallback
            },
            onFailed = {
                // Biometric mismatch
            }
        )
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLockStateHolder.isLockActivityInForeground = false
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_target_package_name"
        const val EXTRA_CLASS_NAME = "extra_target_class_name"

        fun newIntent(context: Context, packageName: String, className: String = ""): Intent {
            return Intent(context, LockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_CLASS_NAME, className)
            }
        }
    }
}
