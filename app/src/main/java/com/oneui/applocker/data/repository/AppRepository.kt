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
            val collator = java.text.Collator.getInstance()
            rawApps.map { app ->
                app.copy(isLocked = lockedSet.contains(app.packageName))
            }.sortedWith(
                compareByDescending<AppItem> { it.isLocked }
                    .thenComparator { a, b -> collator.compare(a.appName, b.appName) }
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
        val collator = java.text.Collator.getInstance()
        raw.map { it.copy(isLocked = lockedSet.contains(it.packageName)) }
            .sortedWith(
                compareByDescending<AppItem> { it.isLocked }
                    .thenComparator { a, b -> collator.compare(a.appName, b.appName) }
            )
    }

    private fun loadInstalledApplications(): List<AppItem> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val infoIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_INFO)
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }
        val resolveInfos = (packageManager.queryIntentActivities(launcherIntent, flags) +
                packageManager.queryIntentActivities(infoIntent, flags))

        return resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val pkg = activityInfo.packageName

            val appInfo = activityInfo.applicationInfo ?: try {
                packageManager.getApplicationInfo(pkg, 0)
            } catch (e: Exception) {
                null
            }

            val appName = (appInfo?.loadLabel(packageManager) ?: resolveInfo.loadLabel(packageManager)).toString().trim()
            val safeAppName = if (appName.isNotBlank()) appName else pkg
            val icon = iconCache.getOrPut(pkg) {
                try {
                    resolveInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
            }
            val isSystem = appInfo?.let {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            } ?: false

            AppItem(
                packageName = pkg,
                appName = safeAppName,
                icon = icon,
                isLocked = false,
                isSystemApp = isSystem
            )
        }.distinctBy { it.packageName }
    }

    fun isAppLocked(packageName: String): Flow<Boolean> = lockedAppDao.isPackageLocked(packageName)

    suspend fun setPackageLockStatus(packageName: String, appName: String, lock: Boolean) = withContext(Dispatchers.IO) {
        if (lock) {
            lockedAppDao.insert(LockedAppEntity(packageName = packageName, appName = appName))
            AppLockStateHolder.addLockedPackage(packageName)
        } else {
            lockedAppDao.deleteByPackageName(packageName)
            AppLockStateHolder.removeLockedPackage(packageName)
        }
    }

    suspend fun setAppLockStatus(app: AppItem, lock: Boolean) = withContext(Dispatchers.IO) {
        setPackageLockStatus(app.packageName, app.appName, lock)
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
