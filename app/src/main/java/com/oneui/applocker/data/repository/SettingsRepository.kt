package com.oneui.applocker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.core.security.RelockPolicy
import com.oneui.applocker.data.model.LockSettings
import com.oneui.applocker.data.model.LockType
import com.oneui.applocker.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oneui_applocker_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val LOCK_TYPE = stringPreferencesKey("lock_type")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val RELOCK_POLICY = stringPreferencesKey("relock_policy")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<LockSettings> = context.dataStore.data.map { preferences ->
        val lockTypeString = preferences[PreferencesKeys.LOCK_TYPE] ?: LockType.PIN.name
        val lockType = try {
            LockType.valueOf(lockTypeString)
        } catch (e: Exception) {
            LockType.PIN
        }

        val biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: true

        val policyString = preferences[PreferencesKeys.RELOCK_POLICY] ?: RelockPolicy.IMMEDIATELY.name
        val policy = try {
            RelockPolicy.valueOf(policyString)
        } catch (e: Exception) {
            RelockPolicy.IMMEDIATELY
        }

        val vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true

        val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        // Keep runtime AppLockStateHolder updated
        AppLockStateHolder.relockPolicy = policy

        LockSettings(
            lockType = lockType,
            isBiometricEnabled = biometricEnabled,
            relockPolicy = policy,
            isVibrationEnabled = vibrationEnabled,
            themeMode = themeMode
        )
    }

    suspend fun setLockType(lockType: LockType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCK_TYPE] = lockType.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setRelockPolicy(policy: RelockPolicy) {
        AppLockStateHolder.relockPolicy = policy
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RELOCK_POLICY] = policy.name
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
}
