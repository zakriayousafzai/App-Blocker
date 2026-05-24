package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BlockedApp
import com.example.data.BlockedAppRepository
import com.example.service.AppBlockerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppBlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BlockedAppRepository
    val blockedApps: StateFlow<List<BlockedApp>>

    init {
        val dao = AppDatabase.getDatabase(application).blockedAppDao()
        repository = BlockedAppRepository(dao)
        blockedApps = repository.allBlockedApps.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // List of common apps for convenient quick-adding
    val predefinedApps = listOf(
        PredefinedApp("Instagram", "com.instagram.android"),
        PredefinedApp("Facebook", "com.facebook.katana"),
        PredefinedApp("YouTube", "com.google.android.youtube"),
        PredefinedApp("TikTok", "com.zhiliaoapp.musically"),
        PredefinedApp("Twitter/X", "com.twitter.android"),
        PredefinedApp("Reddit", "com.reddit.frontpage"),
        PredefinedApp("Snapchat", "com.snapchat.android")
    )

    fun addBlockedApp(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            val app = BlockedApp(
                packageName = packageName.trim(),
                appName = appName.trim(),
                dailyLimitMinutes = limitMinutes,
                timeSpentTodayMs = 0L,
                lastActiveDate = System.currentTimeMillis()
            )
            repository.insertOrUpdate(app)
        }
    }

    fun removeBlockedApp(app: BlockedApp) {
        viewModelScope.launch {
            repository.delete(app)
        }
    }

    fun resetAllLimits() {
        viewModelScope.launch {
            repository.resetAllLimits(System.currentTimeMillis())
        }
    }

    // Start/Stop Foreground tracking service
    fun startLockService(context: Context) {
        val intent = Intent(context, AppBlockerService::class.java).apply {
            action = AppBlockerService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopLockService(context: Context) {
        val intent = Intent(context, AppBlockerService::class.java).apply {
            action = AppBlockerService.ACTION_STOP
        }
        context.startService(intent)
    }
}

data class PredefinedApp(
    val appName: String,
    val packageName: String
)
