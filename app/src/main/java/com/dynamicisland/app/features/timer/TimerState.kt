package com.dynamicisland.app.features.timer

/**
 * Data class representing an active timer or countdown.
 */
data class TimerState(
    val isRunning: Boolean = false,
    val label: String = "Timer",
    val remainingTimeText: String = "",
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val packageName: String = ""
) {
    val isActive: Boolean
        get() = isRunning && remainingTimeText.isNotBlank()
}
