package com.oneui.applocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-initiates lock protection immediately upon phone reboot.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Warm up cache of locked packages from Room DB
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val locked = db.lockedAppDao().getAllLockedPackageNamesSync()
                AppLockStateHolder.updateLockedPackages(locked)

                // Start protection service if permissions are already present
                if (PermissionHelper.hasAllMandatoryPermissions(context)) {
                    AppMonitorForegroundService.start(context)
                }
            }
        }
    }
}
