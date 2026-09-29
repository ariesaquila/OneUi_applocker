package com.oneui.applocker.service

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.oneui.applocker.R
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.ui.MainActivity
import com.oneui.applocker.ui.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service used strictly as a fallback when Accessibility Service is disabled.
 * Uses IMPORTANCE_MIN so it stays completely silent, has no status bar icon, and is minimized.
 */
class AppMonitorForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        registerScreenStateReceiver()
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    AppLockStateHolder.onScreenOff()
                }
            }
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            var lastHandledPackage: String? = null

            while (isActive) {
                if (usageStatsManager != null && PermissionHelper.hasUsageAccessPermission(this@AppMonitorForegroundService)) {
                    val foregroundPackage = getForegroundPackageName(usageStatsManager)

                    if (!foregroundPackage.isNullOrBlank()) {
                        val isLockActivity = (foregroundPackage == packageName && AppLockStateHolder.isLockActivityInForeground)
                        if (!isLockActivity) {
                            if (foregroundPackage != lastHandledPackage) {
                                AppLockStateHolder.onForegroundPackageChanged(foregroundPackage)
                                lastHandledPackage = foregroundPackage
                            }

                            if (AppLockStateHolder.shouldIntercept(foregroundPackage)) {
                                launchLockScreen(foregroundPackage)
                            }
                        }
                    }
                }

                delay(120L)
            }
        }
    }

    private fun getForegroundPackageName(usageStatsManager: UsageStatsManager): String? {
        val currentTime = System.currentTimeMillis()
        try {
            // 1. Primary: Query UsageEvents over the last 15 seconds
            val usageEvents = usageStatsManager.queryEvents(currentTime - 15_000L, currentTime)
            val event = UsageEvents.Event()
            var latestTimestamp = 0L
            var latestPackage: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                // Event type 1 is MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
                if ((event.eventType == 1 || event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) && event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestPackage = event.packageName
                }
            }

            if (!latestPackage.isNullOrBlank()) {
                return latestPackage
            }

            // 2. Secondary fallback: Query UsageStats for devices where events are batched/delayed
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 60_000L,
                currentTime
            )
            if (!stats.isNullOrEmpty()) {
                val mostRecent = stats.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && (currentTime - mostRecent.lastTimeUsed) < 15_000L) {
                    return mostRecent.packageName
                }
            }
        } catch (e: Exception) {
            // UsageStats query failure fallback
        }
        return null
    }

    private fun launchLockScreen(packageName: String) {
        val intent = LockActivity.newIntent(this, packageName).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                packageName.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                options.toBundle()
            )
            try {
                // Pass options.toBundle() to send() so the background activity launch allowance is applied
                pendingIntent.send(this, 0, null, null, null, null, options.toBundle())
            } catch (e: Exception) {
                try {
                    startActivity(intent, options.toBundle())
                } catch (e2: Exception) {
                    startActivity(intent)
                }
            }
        } else {
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foreground_service_channel_name),
                // IMPORTANCE_MIN: No sound, no status bar icon, completely minimized
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.foreground_service_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.notification_protection_active_title))
            .setContentText(getString(R.string.notification_protection_active_content))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "oneui_applocker_silent_monitor"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            // Only start if accessibility is NOT enabled
            if (PermissionHelper.isAccessibilityServiceEnabled(context)) {
                return
            }
            val intent = Intent(context, AppMonitorForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
