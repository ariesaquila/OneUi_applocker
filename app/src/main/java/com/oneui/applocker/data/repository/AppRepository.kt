package com.oneui.applocker.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.data.database.LockedAppDao
import com.oneui.applocker.data.database.LockedAppEntity
import com.oneui.applocker.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AppRepository(
    private val context: Context,
    private val lockedAppDao: LockedAppDao
) {
    private val packageManager: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    /**
     * Streams the full list of installed launchable apps with real-time lock status.
     * Automatically keeps the in-memory AppLockStateHolder synchronized for O(1) service lookups.
     */
    val installedAppsFlow: Flow<List<AppItem>> = lockedAppDao.getAllLockedPackageNames()
        .onEach { lockedPackages ->
            // Keep in-memory cache synchronized with Room DB
            AppLockStateHolder.updateLockedPackages(lockedPackages)
        }
        .combine(getRawInstalledAppsFlow()) { lockedPackageSet, rawApps ->
            val lockedSet = lockedPackageSet.toSet()
            rawApps.map { app ->
                app.copy(isLocked = lockedSet.contains(app.packageName))
            }.sortedWith(
                compareByDescending<AppItem> { it.isLocked }
                    .thenBy { it.appName.lowercase() }
            )
        }
        .flowOn(Dispatchers.IO)

    val lockedCountFlow: Flow<Int> = lockedAppDao.getLockedCount()

    private fun getRawInstalledAppsFlow(): Flow<List<AppItem>> = kotlinx.coroutines.flow.flow {
        val apps = loadInstalledApplications()
        emit(apps)
    }

    suspend fun refreshInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val raw = loadInstalledApplications()
        val lockedSet = lockedAppDao.getAllLockedPackageNamesSync().toSet()
        AppLockStateHolder.updateLockedPackages(lockedSet)
        raw.map { it.copy(isLocked = lockedSet.contains(it.packageName)) }
            .sortedWith(
                compareByDescending<AppItem> { it.isLocked }
                    .thenBy { it.appName.lowercase() }
            )
    }

    private fun loadInstalledApplications(): List<AppItem> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val ownPkg = context.packageName

        return resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val pkg = activityInfo.packageName

            // Exclude our own application from the list of lockable targets
            if (pkg == ownPkg) return@mapNotNull null

            val appInfo = try {
                packageManager.getApplicationInfo(pkg, 0)
            } catch (e: Exception) {
                return@mapNotNull null
            }

            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = iconCache.getOrPut("${pkg}/${activityInfo.name}") {
                resolveInfo.loadIcon(packageManager)
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            AppItem(
                packageName = pkg,
                appName = appName,
                icon = icon,
                isLocked = false,
                isSystemApp = isSystem
            )
        }.distinctBy { "${it.packageName}/${it.appName}" }
    }

    suspend fun setAppLockStatus(app: AppItem, lock: Boolean) = withContext(Dispatchers.IO) {
        if (lock) {
            lockedAppDao.insert(LockedAppEntity(packageName = app.packageName, appName = app.appName))
            AppLockStateHolder.markLocked(app.packageName)
        } else {
            lockedAppDao.deleteByPackageName(app.packageName)
            AppLockStateHolder.markUnlocked(app.packageName)
        }
    }

    suspend fun lockAll(apps: List<AppItem>) = withContext(Dispatchers.IO) {
        val entities = apps.map {
            LockedAppEntity(packageName = it.packageName, appName = it.appName)
        }
        lockedAppDao.insertAll(entities)
        AppLockStateHolder.updateLockedPackages(entities.map { it.packageName })
    }

    suspend fun unlockAll() = withContext(Dispatchers.IO) {
        lockedAppDao.deleteAll()
        AppLockStateHolder.updateLockedPackages(emptyList())
        AppLockStateHolder.clearAllUnlockedSessions()
    }
}
