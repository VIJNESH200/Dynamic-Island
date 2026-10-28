package com.dynamicisland.app.features.music

import android.graphics.Bitmap

/**
 * Immutable state representing current media playback information.
 */
data class MusicState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val packageName: String? = null,
    val isDisplayActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * True if there is an active media track metadata available.
     */
    val hasTrack: Boolean
        get() = title.isNotBlank()

    /**
     * Formats milliseconds into mm:ss string.
     */
    fun formatTime(timeMs: Long): String {
        if (timeMs <= 0) return "0:00"
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    val formattedPosition: String
        get() = formatTime(positionMs)

    val formattedDuration: String
        get() = formatTime(durationMs)

    /**
     * Normalized progress value between 0f and 1f.
     */
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
