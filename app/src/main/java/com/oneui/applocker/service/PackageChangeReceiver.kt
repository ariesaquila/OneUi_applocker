package com.oneui.applocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Removes uninstalled apps from the database automatically.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            db.lockedAppDao().deleteByPackageName(packageName)
            AppLockStateHolder.markLocked(packageName) // removes from locked cache
        }
    }
}
