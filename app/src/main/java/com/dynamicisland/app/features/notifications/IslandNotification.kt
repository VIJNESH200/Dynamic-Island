package com.dynamicisland.app.features.notifications

import android.app.PendingIntent

/**
 * Immutable state representing an incoming notification event to display briefly in the pill.
 */
data class IslandNotification(
    val id: String = "",
    val packageName: String = "",
    val appName: String = "",
    val title: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val contentIntent: PendingIntent? = null
) {
    val isNotEmpty: Boolean
        get() = title.isNotBlank() || text.isNotBlank()
}
