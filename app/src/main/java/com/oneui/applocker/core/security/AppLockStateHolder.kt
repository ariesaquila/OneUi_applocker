package com.oneui.applocker.core.security

import java.util.concurrent.ConcurrentHashMap

enum class RelockPolicy {
    IMMEDIATELY,
    SCREEN_OFF,
    AFTER_ONE_MINUTE,
    AFTER_FIVE_MINUTES
}

/**
 * Thread-safe high-performance singleton maintaining in-memory lock state.
 * Allows O(1) zero-latency lookup for background window monitoring services.
 */
object AppLockStateHolder {

    private val lockedPackages = ConcurrentHashMap.newKeySet<String>()

    // Stores package name -> timestamp of when it was unlocked
    private val unlockedSessions = ConcurrentHashMap<String, Long>()

    // The package that the user is currently using after unlocking
    @Volatile
    var activeUnlockedPackage: String? = null

    // Tracks if the user has reached the target app's window after unlocking
    @Volatile
    var hasEnteredTargetApp: Boolean = false

    @Volatile
    var relockPolicy: RelockPolicy = RelockPolicy.IMMEDIATELY

    @Volatile
    var ownPackageName: String = "com.oneui.applocker"

    @Volatile
    var isLockActivityInForeground: Boolean = false

    @Volatile
    var lastForegroundPackage: String? = null

    fun updateLockedPackages(packages: Collection<String>) {
        lockedPackages.clear()
        lockedPackages.addAll(packages)
    }

    fun isPackageLocked(packageName: String): Boolean {
        return lockedPackages.contains(packageName)
    }

    /**
     * Marks an app as successfully unlocked.
     */
    fun markUnlocked(packageName: String) {
        val now = System.currentTimeMillis()
        unlockedSessions[packageName] = now
        activeUnlockedPackage = packageName
        hasEnteredTargetApp = false
        lastForegroundPackage = packageName
    }

    /**
     * Called when the user opens Recent Apps / Overview or navigates to Home launcher.
     * Immediately locks any active session so content is protected in the task switcher.
     */
    fun onRecentsOrHomeOpened() {
        val current = activeUnlockedPackage
        if (current != null) {
            if (relockPolicy == RelockPolicy.IMMEDIATELY) {
                unlockedSessions.remove(current)
                activeUnlockedPackage = null
                hasEnteredTargetApp = false
            }
        }
    }

    /**
     * Called when the foreground window package changes.
     */
    fun onForegroundPackageChanged(newPackage: String, isRecentsOrHome: Boolean = false) {
        if (newPackage.isBlank()) {
            return
        }

        if (isRecentsOrHome) {
            onRecentsOrHomeOpened()
            lastForegroundPackage = newPackage
            return
        }

        val currentActive = activeUnlockedPackage

        if (currentActive != null) {
            if (currentActive == newPackage) {
                // User has successfully entered and is inside the target app
                hasEnteredTargetApp = true
            } else if (!isTransientInputOrBiometric(newPackage)) {
                // User navigated away to another app or screen
                if (hasEnteredTargetApp && relockPolicy == RelockPolicy.IMMEDIATELY) {
                    unlockedSessions.remove(currentActive)
                    activeUnlockedPackage = null
                    hasEnteredTargetApp = false
                }
            }
        }

        if (unlockedSessions.containsKey(newPackage)) {
            activeUnlockedPackage = newPackage
        }

        lastForegroundPackage = newPackage
    }

    /**
     * Ultra-fast O(1) decision whether to display the lock screen.
     */
    fun shouldIntercept(packageName: String): Boolean {
        if (packageName.isBlank()) {
            return false
        }

        if (isTransientInputOrBiometric(packageName)) {
            return false
        }

        if (!lockedPackages.contains(packageName)) {
            return false
        }

        val unlockTimestamp = unlockedSessions[packageName]

        // If never unlocked or session expired, must intercept
        if (unlockTimestamp == null) {
            return true
        }

        val now = System.currentTimeMillis()

        // 1. Initial Transition Grace: When LockActivity closes, permit entering target app
        if (!hasEnteredTargetApp && activeUnlockedPackage == packageName) {
            return false
        }

        // 2. Active Session: User is actively inside this app
        if (activeUnlockedPackage == packageName) {
            if (relockPolicy == RelockPolicy.AFTER_ONE_MINUTE) {
                return (now - unlockTimestamp) > 60_000L
            }
            if (relockPolicy == RelockPolicy.AFTER_FIVE_MINUTES) {
                return (now - unlockTimestamp) > 300_000L
            }
            return false
        }

        // 3. User left the app and is trying to re-open it
        return when (relockPolicy) {
            RelockPolicy.IMMEDIATELY -> true
            RelockPolicy.AFTER_ONE_MINUTE -> (now - unlockTimestamp) > 60_000L
            RelockPolicy.AFTER_FIVE_MINUTES -> (now - unlockTimestamp) > 300_000L
            RelockPolicy.SCREEN_OFF -> false
        }
    }

    private val transientPackages = setOf(
        "com.samsung.android.biometrics",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller"
    )

    private fun isTransientInputOrBiometric(pkg: String): Boolean {
        return transientPackages.contains(pkg) ||
                pkg.startsWith("com.google.android.inputmethod") ||
                pkg.startsWith("com.samsung.android.honeyboard") ||
                pkg.startsWith("com.touchtype.swiftkey") ||
                pkg.contains("inputmethod")
    }

    fun clearSessionForPackage(packageName: String) {
        unlockedSessions.remove(packageName)
        if (activeUnlockedPackage == packageName) {
            activeUnlockedPackage = null
            hasEnteredTargetApp = false
        }
    }

    fun markLocked(packageName: String) {
        clearSessionForPackage(packageName)
    }

    fun onScreenOff() {
        if (relockPolicy == RelockPolicy.SCREEN_OFF || relockPolicy == RelockPolicy.IMMEDIATELY) {
            unlockedSessions.clear()
            activeUnlockedPackage = null
            hasEnteredTargetApp = false
        }
    }

    fun clearAllUnlockedSessions() {
        unlockedSessions.clear()
        activeUnlockedPackage = null
        hasEnteredTargetApp = false
    }
}
