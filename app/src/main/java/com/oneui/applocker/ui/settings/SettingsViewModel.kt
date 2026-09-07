package com.oneui.applocker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.applocker.AppLockerApp
import com.oneui.applocker.core.security.RelockPolicy
import com.oneui.applocker.data.model.LockSettings
import com.oneui.applocker.data.model.LockType
import com.oneui.applocker.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = (application as AppLockerApp).settingsRepository
    private val securityManager = (application as AppLockerApp).securityManager

    val settingsState: StateFlow<LockSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LockSettings()
    )

    fun onLockTypeSelected(lockType: LockType) {
        viewModelScope.launch {
            settingsRepository.setLockType(lockType)
        }
    }

    fun onBiometricToggled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun onRelockPolicySelected(policy: RelockPolicy) {
        viewModelScope.launch {
            settingsRepository.setRelockPolicy(policy)
        }
    }

    fun onVibrationToggled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun hasPin(): Boolean = securityManager.hasPin()

    fun hasPattern(): Boolean = securityManager.hasPattern()

    fun hasSecurityQuestion(): Boolean = securityManager.hasSecurityQuestion()

    fun getSecurityQuestion(): String? = securityManager.getSecurityQuestion()

    fun saveSecurityQuestion(question: String, answer: String) {
        securityManager.setSecurityQuestion(question, answer)
    }
}
