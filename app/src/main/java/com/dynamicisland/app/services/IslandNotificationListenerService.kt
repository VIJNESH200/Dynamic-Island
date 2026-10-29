package com.dynamicisland.app.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.features.notifications.IslandNotification
import com.dynamicisland.app.features.timer.TimerState

/**
 * NotificationListenerService that listens for incoming notifications
 * to display temporary pill banners on the Dynamic Island, tracks active timers/stopwatches,
 * and provides security context for MediaSessionManager access.
 */
class IslandNotificationListenerService : NotificationListenerService() {

    private val tag = "IslandNotifService"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        try {
            val prefs = IslandPreferences.getInstance(this)
            if (!prefs.isIslandEnabled.value) {
                return
            }

            // Ignore our own notifications
            if (sbn.packageName == packageName) {
                return
            }

            // Check if this notification is an active countdown timer or stopwatch
            if (isTimerNotification(sbn)) {
                if (prefs.isTimerFeatureEnabled.value) {
                    processTimerNotification(sbn)
                }
                return
            }

            if (!prefs.isNotificationFeatureEnabled.value) {
                return
            }

            // Ignore ongoing / persistent notifications for regular notification banners
            if (sbn.isOngoing) {
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Skip empty notifications
            if (title.isBlank() && text.isBlank()) {
                return
            }

            val pm = packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                sbn.packageName
            }

            val islandNotification = IslandNotification(
                id = sbn.key ?: "${sbn.id}",
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            // Forward to the active Dynamic Island overlay
            DynamicIslandService.postNotification(islandNotification)
        } catch (e: Exception) {
            Log.e(tag, "Error processing incoming notification: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return

        try {
            if (isTimerNotification(sbn)) {
                DynamicIslandService.updateTimer(
                    TimerState(
                        isRunning = false,
                        packageName = sbn.packageName
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing notification removal: ${e.message}")
        }
    }

    private fun isTimerNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val pkg = sbn.packageName.lowercase()
        val isClockApp = pkg.contains("deskclock") || pkg.contains("clock") || pkg.contains("timer")
        val category = notification.category?.lowercase() ?: ""
        val isTimerCategory = category == Notification.CATEGORY_STOPWATCH ||
                category == Notification.CATEGORY_ALARM ||
                category == Notification.CATEGORY_PROGRESS ||
                category == "stopwatch" ||
                category == "timer"
        val extras = notification.extras
        val hasChronometer = extras != null && (
                extras.containsKey(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) ||
                extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        )
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""
        val hasTimerKeywords = title.contains("timer") || text.contains("timer") ||
                title.contains("stopwatch") || text.contains("stopwatch")

        return isClockApp || isTimerCategory || hasChronometer || hasTimerKeywords
    }

    private fun processTimerNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val timeRegex = Regex(".*\\d+:\\d+.*")
        val remainingText = when {
            rawText.matches(timeRegex) -> rawText
            subText.matches(timeRegex) -> subText
            rawTitle.matches(timeRegex) -> rawTitle
            rawText.isNotBlank() -> rawText
            else -> "Running"
        }

        val label = if (rawTitle.matches(timeRegex) || rawTitle.isBlank()) "Timer" else rawTitle
        val isPaused = rawText.contains("pause", ignoreCase = true) || rawTitle.contains("pause", ignoreCase = true)
        val isRunning = sbn.isOngoing && !isPaused

        val timerState = TimerState(
            isRunning = isRunning,
            label = label,
            remainingTimeText = remainingText,
            packageName = sbn.packageName
        )

        DynamicIslandService.updateTimer(timerState)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(tag, "Notification listener disconnected")
    }
}
