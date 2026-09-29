package com.oneui.applocker

import android.app.Application
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.core.security.SecurityManager
import com.oneui.applocker.data.database.AppDatabase
import com.oneui.applocker.data.repository.AppRepository
import com.oneui.applocker.data.repository.SettingsRepository
import com.oneui.applocker.service.AppMonitorForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLockerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var appRepository: AppRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var securityManager: SecurityManager
        private set

    override fun attachBaseContext(base: android.content.Context) {
        val lang = com.oneui.applocker.core.util.LocaleHelper.getSavedLanguage(base)
        super.attachBaseContext(com.oneui.applocker.core.util.LocaleHelper.applyLocale(base, lang))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        AppLockStateHolder.ownPackageName = packageName

        // Initialize dependencies
        database = AppDatabase.getInstance(this)
        appRepository = AppRepository(this, database.lockedAppDao())
        settingsRepository = SettingsRepository(this)
        securityManager = SecurityManager(this)

        // Pre-warm locked apps cache into memory for O(1) service verification
        CoroutineScope(Dispatchers.IO).launch {
            val lockedApps = database.lockedAppDao().getAllLockedPackageNamesSync()
            AppLockStateHolder.updateLockedPackages(lockedApps)

            if (PermissionHelper.hasUsageAccessPermission(this@AppLockerApp)) {
                AppMonitorForegroundService.start(this@AppLockerApp)
            }
        }
    }

    companion object {
        lateinit var instance: AppLockerApp
            private set
    }
}
