package com.oneui.applocker.core.permission

import androidx.annotation.StringRes
import com.oneui.applocker.R

enum class PermissionType(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val isRequired: Boolean
) {
    OVERLAY(
        titleRes = R.string.perm_overlay_title,
        descRes = R.string.perm_overlay_desc,
        isRequired = true
    ),
    USAGE_ACCESS(
        titleRes = R.string.perm_usage_title,
        descRes = R.string.perm_usage_desc,
        isRequired = true
    ),
    BATTERY_OPTIMIZATION(
        titleRes = R.string.perm_battery_title,
        descRes = R.string.perm_battery_desc,
        isRequired = false // Highly recommended
    ),
    NOTIFICATION(
        titleRes = R.string.perm_notification_title,
        descRes = R.string.perm_notification_desc,
        isRequired = true
    )
}
