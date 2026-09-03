package com.oneui.applocker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val lockedAt: Long = System.currentTimeMillis()
)
