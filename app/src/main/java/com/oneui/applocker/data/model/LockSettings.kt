package com.oneui.applocker.data.model

import com.oneui.applocker.core.security.RelockPolicy

enum class LockType {
    PIN,
    PATTERN
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class LockSettings(
    val lockType: LockType = LockType.PIN,
    val isBiometricEnabled: Boolean = true,
    val relockPolicy: RelockPolicy = RelockPolicy.IMMEDIATELY,
    val isVibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
