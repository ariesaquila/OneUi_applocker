package com.oneui.applocker.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_TR = "tr"
    const val LANGUAGE_EN = "en"

    fun setLanguage(languageCode: String) {
        val appLocale = when (languageCode) {
            LANGUAGE_TR -> LocaleListCompat.forLanguageTags("tr")
            LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLanguageCode(): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty) {
            return LANGUAGE_SYSTEM
        }
        val tag = currentLocales[0]?.language ?: ""
        return when {
            tag.startsWith("tr", ignoreCase = true) -> LANGUAGE_TR
            tag.startsWith("en", ignoreCase = true) -> LANGUAGE_EN
            else -> LANGUAGE_SYSTEM
        }
    }
}
