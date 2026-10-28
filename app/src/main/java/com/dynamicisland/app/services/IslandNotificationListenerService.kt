package com.dynamicisland.app.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.features.notifications.IslandNotification

/**
 * NotificationListenerService that listens for incoming notifications
 * to display temporary pill banners on the Dynamic Island, and provides
 * security context for MediaSessionManager access.
 */
class IslandNotificationListenerService : NotificationListenerService() {

    private val tag = "IslandNotifService"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        try {
            val prefs = IslandPreferences.getInstance(this)
            if (!prefs.isIslandEnabled.value || !prefs.isNotificationFeatureEnabled.value) {
                return
            }

            // Ignore our own notifications
            if (sbn.packageName == packageName) {
                return
            }

            // Ignore ongoing / persistent notifications
            if (sbn.isOngoing) {
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Skip empty or progress notifications
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

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(tag, "Notification listener disconnected")
    }
}
