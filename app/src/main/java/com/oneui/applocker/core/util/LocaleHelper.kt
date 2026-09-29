package com.oneui.applocker.core.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_TR = "tr"
    const val LANGUAGE_EN = "en"

    private const val PREFS_NAME = "oneui_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        // Sync with AppCompat / Android 13+ Per-App Language
        val appLocale = when (languageCode) {
            LANGUAGE_TR -> LocaleListCompat.forLanguageTags("tr")
            LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun applyLocale(context: Context, languageCode: String = getSavedLanguage(context)): Context {
        val targetLocale = when (languageCode) {
            LANGUAGE_TR -> Locale("tr")
            LANGUAGE_EN -> Locale("en")
            else -> {
                val sysLocales = ConfigurationCompat.getLocales(context.resources.configuration)
                if (!sysLocales.isEmpty) sysLocales[0] ?: Locale.getDefault() else Locale.getDefault()
            }
        }

        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        return context.createConfigurationContext(config)
    }
}
