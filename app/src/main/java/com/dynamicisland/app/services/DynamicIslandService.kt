package com.dynamicisland.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dynamicisland.app.R
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.features.battery.BatteryManagerHelper
import com.dynamicisland.app.features.music.MusicControllerHelper
import com.dynamicisland.app.features.notifications.IslandNotification
import com.dynamicisland.app.features.timer.TimerState
import com.dynamicisland.app.overlay.DynamicIslandOverlay
import com.dynamicisland.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps the Dynamic Island overlay alive.
 * Configured with START_STICKY for clean recovery on Nothing OS / Android 14/15.
 */
class DynamicIslandService : Service() {

    private val tag = "DynamicIslandService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var preferences: IslandPreferences
    private lateinit var batteryHelper: BatteryManagerHelper
    private lateinit var musicHelper: MusicControllerHelper
    private var overlay: DynamicIslandOverlay? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "DynamicIslandService onCreate")

        preferences = IslandPreferences.getInstance(this)
        batteryHelper = BatteryManagerHelper(this)
        musicHelper = MusicControllerHelper(this)

        startForegroundNotification()

        // Initialize features
        batteryHelper.startMonitoring()
        musicHelper.start()

        // Listen for power connected/disconnected to show charging pill for 4s
        batteryHelper.onPowerStateChangedListener = { _ ->
            if (preferences.isBatteryFeatureEnabled.value) {
                overlay?.triggerChargerAlert()
            }
        }

        // Listen for music playback state changes
        serviceScope.launch {
            combine(
                musicHelper.musicState,
                preferences.isMusicFeatureEnabled
            ) { musicState, isEnabled ->
                val trackId = "${musicState.packageName}:${musicState.title}:${musicState.artist}"
                Pair(isEnabled && musicState.isDisplayActive, trackId)
            }.distinctUntilChanged()
            .collect { (shouldShowMusic, trackId) ->
                overlay?.setMusicActive(shouldShowMusic, trackId)
            }
        }

        // Initialize and show overlay if overlay permission is granted
        ensureOverlay()

        _isServiceRunning.value = true
    }

    private var isPreferencesCollectorStarted = false

    private fun ensureOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(tag, "Cannot start overlay: SYSTEM_ALERT_WINDOW permission not granted.")
            return
        }

        if (overlay == null) {
            val newOverlay = DynamicIslandOverlay(
                context = this,
                preferences = preferences,
                batteryHelper = batteryHelper,
                musicHelper = musicHelper
            )
            overlay = newOverlay
            activeOverlayInstance = newOverlay
        }

        if (!isPreferencesCollectorStarted) {
            isPreferencesCollectorStarted = true
            serviceScope.launch {
                combine(
                    preferences.isIslandEnabled,
                    preferences.isBatteryFeatureEnabled,
                    preferences.isMusicFeatureEnabled,
                    preferences.isNotificationFeatureEnabled,
                    preferences.isTimerFeatureEnabled
                ) { isIsland, _, _, _, _ ->
                    isIsland
                }.collect { isEnabled ->
                    if (isEnabled) {
                        overlay?.updateOverlayVisibility()
                    } else {
                        overlay?.hide()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "DynamicIslandService onStartCommand with startId: $startId")

        // Handle stop action if sent from pending intent or toggle
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Re-check overlay if it wasn't added yet (e.g. permission was just granted)
        ensureOverlay()

        // START_STICKY tells Android to recreate the service cleanly if killed by OS memory trimmer
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(tag, "App task removed from recent apps, service continues running")
        // No action needed; service remains alive as a foreground service
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "DynamicIslandService onDestroy")

        activeOverlayInstance = null
        overlay?.hide()
        overlay = null

        batteryHelper.stopMonitoring()
        musicHelper.stop()

        isPreferencesCollectorStarted = false
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    private fun startForegroundNotification() {
        createNotificationChannel()

        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        // Android 14+ (API 34+) requires specifying the foreground service type at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "dynamic_island_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.dynamicisland.app.ACTION_STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        @Volatile
        private var activeOverlayInstance: DynamicIslandOverlay? = null

        fun start(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun postNotification(notification: IslandNotification) {
            activeOverlayInstance?.postNotificationAlert(notification)
        }

        fun updateTimer(timer: TimerState) {
            activeOverlayInstance?.updateTimer(timer)
        }

        fun triggerChargerAlert() {
            activeOverlayInstance?.triggerChargerAlert()
        }

        fun triggerManualPreview() {
            activeOverlayInstance?.triggerManualPreview()
        }
    }
}
