package com.oneui.applocker.data.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isLocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val installedTimestamp: Long = 0L
)
