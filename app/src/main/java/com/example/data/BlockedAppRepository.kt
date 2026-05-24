package com.example.data

import kotlinx.coroutines.flow.Flow

class BlockedAppRepository(private val dao: BlockedAppDao) {
    val allBlockedApps: Flow<List<BlockedApp>> = dao.getAllBlockedAppsFlow()

    suspend fun getBlockedAppsList(): List<BlockedApp> = dao.getAllBlockedApps()

    suspend fun getBlockedApp(packageName: String): BlockedApp? = dao.getBlockedApp(packageName)

    suspend fun insertOrUpdate(blockedApp: BlockedApp) = dao.insertOrUpdate(blockedApp)

    suspend fun updateTimeSpent(packageName: String, timeSpent: Long, lastActive: Long) =
        dao.updateTimeSpent(packageName, timeSpent, lastActive)

    suspend fun resetAllLimits(currentMillis: Long) = dao.resetAllLimits(currentMillis)

    suspend fun delete(blockedApp: BlockedApp) = dao.delete(blockedApp)
}
