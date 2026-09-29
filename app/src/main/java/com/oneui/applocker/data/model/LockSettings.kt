package com.oneui.applocker.data.model

import androidx.compose.runtime.Immutable
import com.oneui.applocker.core.security.RelockPolicy

enum class LockType {
    PIN,
    PATTERN
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

@Immutable
data class LockSettings(
    val lockType: LockType = LockType.PIN,
    val isBiometricEnabled: Boolean = true,
    val relockPolicy: RelockPolicy = RelockPolicy.IMMEDIATELY,
    val isVibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
