package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps")
    fun getAllBlockedAppsFlow(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAllBlockedApps(): List<BlockedApp>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getBlockedApp(packageName: String): BlockedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(blockedApp: BlockedApp)

    @Query("UPDATE blocked_apps SET timeSpentTodayMs = :timeSpent, lastActiveDate = :lastActive WHERE packageName = :packageName")
    suspend fun updateTimeSpent(packageName: String, timeSpent: Long, lastActive: Long)

    @Query("UPDATE blocked_apps SET timeSpentTodayMs = 0, lastActiveDate = :currentMillis")
    suspend fun resetAllLimits(currentMillis: Long)

    @Delete
    suspend fun delete(blockedApp: BlockedApp)
}
