package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.BlockedApp
import com.example.data.BlockedAppRepository
import java.util.Calendar
import kotlinx.coroutines.*

class AppBlockerService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        private const val CHECK_INTERVAL_MS = 1500L
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "AppBlockerServiceChannel"

        @Volatile
        var isRunning = false
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: BlockedAppRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var windowManager: WindowManager

    private var tickerJob: Job? = null
    private var isReceiverRegistered = false
    private var overlayInfo: OverlayInfo? = null

    private data class OverlayInfo(
        val view: ComposeView,
        val lifecycleOwner: ServiceLifecycleOwner,
        val packageName: String
    )

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    stopTrackingLoop()
                }
                Intent.ACTION_USER_PRESENT -> {
                    startTrackingLoop()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = BlockedAppRepository(db.blockedAppDao())
        sharedPreferences = getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Run as foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        startTrackingLoop()
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTrackingLoop()
        unregisterScreenReceiver()
        removeActiveOverlay()
        serviceJob.cancel()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTrackingLoop() {
        if (tickerJob?.isActive == true) return

        tickerJob = serviceScope.launch {
            while (isActive) {
                checkAndResetDailyLimitsIfNewDay()
                val currentPackage = getForegroundPackageName()
                if (currentPackage != null) {
                    processForegroundPackage(currentPackage)
                } else {
                    removeActiveOverlay()
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun stopTrackingLoop() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private suspend fun processForegroundPackage(packageName: String) {
        if (packageName == this.packageName || isLauncherApp(packageName)) {
            removeActiveOverlay()
            return
        }

        val blockedApp = repository.getBlockedApp(packageName)
        if (blockedApp == null) {
            removeActiveOverlay()
            return
        }

        val limitMs = blockedApp.dailyLimitMinutes * 60 * 1000L

        if (blockedApp.timeSpentTodayMs >= limitMs) {
            showOverlay(blockedApp)
            return
        }

        val newTimeSpent = blockedApp.timeSpentTodayMs + CHECK_INTERVAL_MS
        repository.updateTimeSpent(packageName, newTimeSpent, System.currentTimeMillis())

        if (newTimeSpent >= limitMs) {
            showOverlay(blockedApp)
        } else {
            removeActiveOverlay()
        }
    }

    private fun showOverlay(blockedApp: BlockedApp) {
        if (overlayInfo?.packageName == blockedApp.packageName) return

        removeActiveOverlay()

        val context = this
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.start()

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                AppBlockerOverlayContent(
                    appName = blockedApp.appName,
                    limitMinutes = blockedApp.dailyLimitMinutes,
                    onGoHome = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                    }
                )
            }
        }

        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(composeView, params)
            overlayInfo = OverlayInfo(composeView, lifecycleOwner, blockedApp.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeActiveOverlay() {
        overlayInfo?.let {
            try {
                windowManager.removeView(it.view)
                it.lifecycleOwner.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayInfo = null
    }

    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60000

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime) ?: return null
        val event = UsageEvents.Event()
        var foregroundPackage: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            }
        }
        return foregroundPackage
    }

    private fun isLauncherApp(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun checkAndResetDailyLimitsIfNewDay() {
        val lastResetTime = sharedPreferences.getLong("last_reset_time", 0L)
        val now = System.currentTimeMillis()

        val calLast = Calendar.getInstance().apply { timeInMillis = lastResetTime }
        val calNow = Calendar.getInstance().apply { timeInMillis = now }

        val isDifferentDay = calLast.get(Calendar.YEAR) != calNow.get(Calendar.YEAR) ||
                calLast.get(Calendar.DAY_OF_YEAR) != calNow.get(Calendar.DAY_OF_YEAR)

        if (lastResetTime == 0L || isDifferentDay) {
            serviceScope.launch {
                repository.resetAllLimits(now)
                sharedPreferences.edit().putLong("last_reset_time", now).apply()
            }
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
        isReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (isReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            isReceiverRegistered = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app limits and displays overlay screens."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocker Service")
            .setContentText("Digital wellness active: enforcing custom limits.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
