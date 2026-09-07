package com.oneui.applocker.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.oneui.applocker.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = OneUiCardLight,
    primaryContainer = OneUiBlueDark,
    onPrimaryContainer = OneUiCardLight,
    background = OneUiBgDark,
    onBackground = OneUiTextPrimaryDark,
    surface = OneUiCardDark,
    onSurface = OneUiTextPrimaryDark,
    surfaceVariant = OneUiCardDark,
    onSurfaceVariant = OneUiTextSecondaryDark,
    outline = OneUiDividerDark,
    error = OneUiRed
)

private val LightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    onPrimary = OneUiCardLight,
    primaryContainer = OneUiBlueLight,
    onPrimaryContainer = OneUiBlueDark,
    background = OneUiBgLight,
    onBackground = OneUiTextPrimaryLight,
    surface = OneUiCardLight,
    onSurface = OneUiTextPrimaryLight,
    surfaceVariant = OneUiCardLight,
    onSurfaceVariant = OneUiTextSecondaryLight,
    outline = OneUiDividerLight,
    error = OneUiRed
)

private val AmoledColorScheme = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = Color.White,
    primaryContainer = OneUiBlueDark,
    onPrimaryContainer = Color.White,
    background = OneUiBgAmoled,
    onBackground = OneUiTextPrimaryAmoled,
    surface = OneUiCardAmoled,
    onSurface = OneUiTextPrimaryAmoled,
    surfaceVariant = OneUiCardAmoled,
    onSurfaceVariant = OneUiTextSecondaryAmoled,
    outline = OneUiDividerAmoled,
    error = OneUiRed
)

@Composable
fun OneUiAppLockerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default false to preserve distinctive One UI blue aesthetic
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        themeMode == ThemeMode.AMOLED -> AmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OneUiTypography,
        shapes = OneUiShapes,
        content = content
    )
}
