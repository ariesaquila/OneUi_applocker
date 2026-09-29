package com.oneui.applocker.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {

    @Query("SELECT * FROM locked_apps ORDER BY lockedAt DESC")
    fun getAllLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps")
    suspend fun getAllLockedAppsSync(): List<LockedAppEntity>

    @Query("SELECT packageName FROM locked_apps")
    fun getAllLockedPackageNames(): Flow<List<String>>

    @Query("SELECT packageName FROM locked_apps")
    suspend fun getAllLockedPackageNamesSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<LockedAppEntity>)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM locked_apps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM locked_apps")
    fun getLockedCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName)")
    fun isPackageLocked(packageName: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName)")
    suspend fun isPackageLockedSync(packageName: String): Boolean
}
