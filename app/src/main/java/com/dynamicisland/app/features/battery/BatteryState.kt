package com.dynamicisland.app.features.battery

import android.os.BatteryManager

/**
 * Represents the charging power source.
 */
enum class PowerSource {
    AC,
    USB,
    WIRELESS,
    NONE
}

/**
 * Immutable state representing the battery information.
 */
data class BatteryState(
    val level: Int = 100,
    val scale: Int = 100,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val powerSource: PowerSource = PowerSource.NONE,
    val chargeTimeRemainingMillis: Long = -1L,
    val temperatureCelsius: Float = 25f,
    val health: Int = BatteryManager.BATTERY_HEALTH_GOOD,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Percentage value clamped between 0 and 100.
     */
    val percentage: Int
        get() = if (scale > 0) ((level.toFloat() / scale) * 100).toInt().coerceIn(0, 100) else level.coerceIn(0, 100)

    /**
     * Short label for the collapsed pill (e.g., "85%").
     */
    val shortPercentageText: String
        get() = "$percentage%"

    /**
     * Formats charging time remaining into human-readable text.
     * e.g., "42m to full" or "Fast charging"
     */
    fun formatRemainingTime(): String {
        if (!isCharging) return "On battery"
        if (isFull || percentage >= 100) return "Fully Charged"
        
        if (chargeTimeRemainingMillis > 0) {
            val totalMinutes = chargeTimeRemainingMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m to full"
                hours > 0 -> "${hours}h to full"
                minutes > 0 -> "${minutes}m to full"
                else -> "Charging"
            }
        }

        return when (powerSource) {
            PowerSource.AC -> "Fast Charging (AC)"
            PowerSource.USB -> "Charging (USB)"
            PowerSource.WIRELESS -> "Wireless Charging"
            PowerSource.NONE -> "Charging"
        }
    }

    /**
     * Returns a human-readable power source description.
     */
    fun powerSourceDescription(): String {
        return when (powerSource) {
            PowerSource.AC -> "AC Fast Charger"
            PowerSource.USB -> "USB Cable"
            PowerSource.WIRELESS -> "Wireless Dock"
            PowerSource.NONE -> "Not Connected"
        }
    }
}
