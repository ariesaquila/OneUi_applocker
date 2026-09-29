package com.oneui.applocker.service

import android.app.ActivityOptions
import android.app.AlarmManager
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
 * High-performance foreground monitoring service for application locking.
 * Uses UsageEvents polling + Full-Screen Intent fallback for guaranteed immediate interception.
 */
class AppMonitorForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundServiceNotification()
        registerScreenStateReceiver()
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) {
            startMonitoringLoop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep service alive if user swipes One UI AppLocker away from recent apps
        try {
            val restartIntent = Intent(applicationContext, AppMonitorForegroundService::class.java)
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                101,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000L,
                restartPendingIntent
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

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

    private fun isIgnoredPackage(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return true
        if (pkg == "android" || pkg == "com.android.systemui") return true
        if (pkg.contains("inputmethod") || pkg.contains("honeyboard") || pkg.contains("swiftkey")) return true
        return false
    }

    private fun getForegroundPackageName(usageStatsManager: UsageStatsManager): String? {
        val currentTime = System.currentTimeMillis()
        try {
            // Query events over the last 60 seconds
            val usageEvents = usageStatsManager.queryEvents(currentTime - 60_000L, currentTime)
            val event = UsageEvents.Event()
            var latestTimestamp = 0L
            var latestPackage: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                // Event type 1 is MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
                if ((event.eventType == 1 || event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) && event.timeStamp >= latestTimestamp) {
                    val pkg = event.packageName
                    if (!isIgnoredPackage(pkg)) {
                        latestTimestamp = event.timeStamp
                        latestPackage = pkg
                    }
                }
            }

            if (!latestPackage.isNullOrBlank()) {
                return latestPackage
            }

            // Fallback for devices where events are batched:
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 60_000L,
                currentTime
            )
            if (!stats.isNullOrEmpty()) {
                val mostRecent = stats.filter { !isIgnoredPackage(it.packageName) }.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && (currentTime - mostRecent.lastTimeUsed) < 20_000L) {
                    return mostRecent.packageName
                }
            }
        } catch (e: Exception) {
            // Fallback error handling
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

        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
        } else {
            ActivityOptions.makeBasic()
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            packageName.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            options.toBundle()
        )

        // Tier 1: Direct startActivity
        try {
            startActivity(intent, options.toBundle())
            return
        } catch (e: Exception) {
            // Fallback
        }

        // Tier 2: PendingIntent.send
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                pendingIntent.send(this, 0, null, null, null, null, options.toBundle())
            } else {
                pendingIntent.send()
            }
            return
        } catch (e: Exception) {
            // Fallback
        }

        // Tier 3: Full-Screen Intent notification (Guaranteed foreground popup on Android 10-15)
        try {
            val notification = NotificationCompat.Builder(this, LOCK_ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle(getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(LOCK_ALERT_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            try {
                startActivity(intent)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            // 1. Silent channel for ongoing background monitoring
            val silentChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foreground_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.foreground_service_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            manager.createNotificationChannel(silentChannel)

            // 2. High-priority alert channel for Full-Screen Intent lock popup
            val alertChannel = NotificationChannel(
                LOCK_ALERT_CHANNEL_ID,
                "One UI App Locker Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lock screen triggers"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(alertChannel)
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
        const val LOCK_ALERT_CHANNEL_ID = "oneui_applocker_lock_alert"
        const val NOTIFICATION_ID = 1001
        const val LOCK_ALERT_NOTIFICATION_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
