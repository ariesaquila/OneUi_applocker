package com.oneui.applocker.ui.lock

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.applocker.AppLockerApp
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.core.security.SecurityManager
import com.oneui.applocker.data.model.LockSettings
import com.oneui.applocker.data.model.LockType
import com.oneui.applocker.data.model.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class LockUiEvent {
    data object UnlockSuccess : LockUiEvent()
    data class TriggerBiometric(val title: String) : LockUiEvent()
}

data class LockUiState(
    val targetPackage: String = "",
    val appName: String = "",
    val appIcon: Drawable? = null,
    val lockType: LockType = LockType.PIN,
    val isBiometricEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val enteredPin: String = "",
    val isError: Boolean = false,
    val errorMessage: String? = null
)

class LockViewModel(
    application: Application,
    val targetPackageName: String,
    val targetClassName: String = ""
) : AndroidViewModel(application) {

    private val securityManager: SecurityManager = (application as AppLockerApp).securityManager
    private val settingsRepository = (application as AppLockerApp).settingsRepository
    private val packageManager: PackageManager = application.packageManager

    private val _uiState = MutableStateFlow(LockUiState(targetPackage = targetPackageName))
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LockUiEvent>()
    val eventFlow: SharedFlow<LockUiEvent> = _eventFlow.asSharedFlow()

    init {
        loadAppInfoAndSettings()
    }

    private fun loadAppInfoAndSettings() {
        viewModelScope.launch {
            // First attempt to load specific activity info (e.g. Gemini component within Google package)
            val appName = try {
                if (targetClassName.isNotBlank()) {
                    val component = ComponentName(targetPackageName, targetClassName)
                    val activityInfo = packageManager.getActivityInfo(component, 0)
                    activityInfo.loadLabel(packageManager).toString()
                } else {
                    val appInfo = packageManager.getApplicationInfo(targetPackageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                }
            } catch (e: Exception) {
                try {
                    val appInfo = packageManager.getApplicationInfo(targetPackageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e2: Exception) {
                    targetPackageName
                }
            }

            val icon = try {
                if (targetClassName.isNotBlank()) {
                    val component = ComponentName(targetPackageName, targetClassName)
                    val activityInfo = packageManager.getActivityInfo(component, 0)
                    activityInfo.loadIcon(packageManager)
                } else {
                    packageManager.getApplicationIcon(targetPackageName)
                }
            } catch (e: Exception) {
                try {
                    packageManager.getApplicationIcon(targetPackageName)
                } catch (e2: Exception) {
                    null
                }
            }

            val settings: LockSettings = settingsRepository.settingsFlow.first()

            _uiState.value = _uiState.value.copy(
                appName = appName,
                appIcon = icon,
                lockType = settings.lockType,
                isBiometricEnabled = settings.isBiometricEnabled,
                isVibrationEnabled = settings.isVibrationEnabled,
                themeMode = settings.themeMode
            )

            // Trigger biometric automatically on appearance if enabled
            if (settings.isBiometricEnabled) {
                _eventFlow.emit(LockUiEvent.TriggerBiometric(appName))
            }
        }
    }

    fun onPinDigit(digit: String) {
        val current = _uiState.value.enteredPin
        if (current.length < 4) {
            val newPin = current + digit
            _uiState.value = _uiState.value.copy(enteredPin = newPin, isError = false)

            if (newPin.length == 4) {
                verifyPin(newPin)
            }
        }
    }

    fun onPinDelete() {
        val current = _uiState.value.enteredPin
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                enteredPin = current.dropLast(1),
                isError = false
            )
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val isValid = securityManager.verifyPin(pin)
            if (isValid) {
                onUnlockSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    errorMessage = "Hatalı PIN"
                )
                delay(600)
                _uiState.value = _uiState.value.copy(
                    enteredPin = "",
                    isError = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onPatternComplete(pattern: List<Int>) {
        viewModelScope.launch {
            val isValid = securityManager.verifyPattern(pattern)
            if (isValid) {
                onUnlockSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    errorMessage = "Hatalı desen"
                )
                delay(700)
                _uiState.value = _uiState.value.copy(
                    isError = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onBiometricSuccess() {
        onUnlockSuccess()
    }

    private fun onUnlockSuccess() {
        AppLockStateHolder.markUnlocked(targetPackageName)
        viewModelScope.launch {
            _eventFlow.emit(LockUiEvent.UnlockSuccess)
        }
    }

    fun requestBiometricPrompt() {
        viewModelScope.launch {
            _eventFlow.emit(LockUiEvent.TriggerBiometric(_uiState.value.appName))
        }
    }
}
