package com.oneui.applocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.oneui.applocker.core.security.AppLockStateHolder
import com.oneui.applocker.ui.lock.LockActivity

/**
 * Ultra-fast event-driven Accessibility Service for 0-latency app locking.
 * Intercepts window state changes directly from the Android Window Manager.
 */
class AppLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val targetPackage = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Skip our own app
        if (targetPackage == packageName) {
            return
        }

        // Detect if user opened the Recent Apps / Task Switcher or Home screen
        val isRecentsOrHome = className.contains("Recents", ignoreCase = true) ||
                className.contains("Overview", ignoreCase = true) ||
                className.contains("TaskSwitcher", ignoreCase = true) ||
                className.contains("QuickStep", ignoreCase = true) ||
                className.contains("Launcher", ignoreCase = true)

        // Notify state holder of foreground or task switcher transition
        AppLockStateHolder.onForegroundPackageChanged(targetPackage, isRecentsOrHome)

        // If not in recents view, check whether target app must be intercepted
        if (!isRecentsOrHome && AppLockStateHolder.shouldIntercept(targetPackage)) {
            launchLockScreen(targetPackage, className)
        }
    }

    private fun launchLockScreen(targetPackageName: String, className: String = "") {
        val intent = LockActivity.newIntent(this, targetPackageName, className).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Service interrupted by system
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLockStateHolder.ownPackageName = packageName
    }
}
