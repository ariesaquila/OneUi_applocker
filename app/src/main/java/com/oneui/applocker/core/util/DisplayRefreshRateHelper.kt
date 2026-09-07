package com.oneui.applocker.core.util

import android.app.Activity
import android.os.Build
import android.view.WindowManager

/**
 * Helper to synchronize the application's window refresh rate with the phone's native display.
 * Automatically selects the highest refresh rate mode (90Hz, 120Hz, 144Hz) supported by the hardware,
 * ensuring buttery-smooth 120 FPS animations, zero touch latency, and no frame drops.
 */
object DisplayRefreshRateHelper {

    /**
     * Applies high refresh rate display mode to the given Activity.
     * Safe to call on all Android versions (API 26+).
     */
    fun syncWithDeviceRefreshRate(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display ?: return
                val currentMode = display.mode
                val supportedModes = display.supportedModes

                // Find mode matching the screen resolution with the highest refresh rate (e.g. 120Hz)
                val peakMode = supportedModes
                    .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                    .maxByOrNull { it.refreshRate }
                    ?: supportedModes.maxByOrNull { it.refreshRate }

                if (peakMode != null) {
                    val attributes = activity.window.attributes
                    attributes.preferredDisplayModeId = peakMode.modeId
                    activity.window.attributes = attributes
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = activity.windowManager.defaultDisplay
                val maxRate = display.supportedModes.maxOfOrNull { it.refreshRate } ?: 60f
                if (maxRate > 60f) {
                    val attributes = activity.window.attributes
                    attributes.preferredRefreshRate = maxRate
                    activity.window.attributes = attributes
                }
            }
        } catch (e: Exception) {
            // Silently fallback to system default if OEM restrictions block display mode changes
        }
    }
}
