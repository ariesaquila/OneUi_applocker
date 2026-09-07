package com.oneui.applocker.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Authentic Samsung One UI Typography System.
 * Uses [FontFamily.SansSerif] so it natively reflects Samsung Sans / One UI Sans on Galaxy devices
 * and standard system sans-serif on other Android phones.
 *
 * Proportionally calibrated for smartphone ergonomics: prevents text clipping, avoids awkward line-wraps,
 * and maintains clean visual hierarchy across all screen densities.
 */
private val OneUiFontFamily = FontFamily.SansSerif

val OneUiTypography = Typography(
    // One UI Large Viewing Area Header (e.g. "Uygulama Kilidi", "Ayarlar")
    headlineLarge = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    // Prominent modal or dialog headers
    headlineMedium = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    // Lock screen app name, pin setup title
    headlineSmall = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    // Card group headers or section titles
    titleLarge = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    // Primary item title in list cards (e.g. App Name, Setting Name)
    titleMedium = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    // Compact item title or sub-items
    titleSmall = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Primary reading text
    bodyLarge = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    // Subtitles, descriptive paragraphs, card subtitles
    bodyMedium = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    // Captions, package names, tips
    bodySmall = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Primary action buttons
    labelLarge = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    // Filter chips, badges, secondary buttons
    labelMedium = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Very small tags (e.g. "Sistem" tag)
    labelSmall = TextStyle(
        fontFamily = OneUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)
