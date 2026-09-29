package com.oneui.applocker.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.oneui.applocker.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun getLocaleForLanguage(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.TURKISH -> Locale("tr")
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.SYSTEM -> {
                val systemLocales = ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration)
                if (!systemLocales.isEmpty) {
                    systemLocales[0] ?: Locale.getDefault()
                } else {
                    Locale.getDefault()
                }
            }
        }
    }

    fun applyLocale(context: Context, language: AppLanguage) {
        val localeListCompat = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(localeListCompat)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (language == AppLanguage.SYSTEM) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList(Locale.forLanguageTag(language.code))
            }
        }
    }

    fun wrapContext(context: Context, language: AppLanguage): Context {
        val locale = getLocaleForLanguage(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
