package com.dynamicisland.app.features.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper class that monitors battery changes via BroadcastReceiver and BatteryManager APIs.
 * Supports Android 8.0 (API 26) up to Android 15 (API 35).
 */
class BatteryManagerHelper(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    // Callback when power is connected (to trigger island attention)
    var onPowerConnectedListener: (() -> Unit)? = null

    private var isReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    updateBatteryFromIntent(intent)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    onPowerConnectedListener?.invoke()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    // Update state to reflect disconnection
                    val current = _batteryState.value
                    _batteryState.value = current.copy(
                        isCharging = false,
                        powerSource = PowerSource.NONE,
                        chargeTimeRemainingMillis = -1L
                    )
                }
            }
        }
    }

    /**
     * Start monitoring battery broadcasts.
     */
    fun startMonitoring() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        // Use ContextCompat.registerReceiver with RECEIVER_EXPORTED for Android 14/15 compatibility
        val stickyIntent = androidx.core.content.ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        isReceiverRegistered = true
        stickyIntent?.let { updateBatteryFromIntent(it) }
    }

    /**
     * Stop monitoring battery broadcasts to avoid memory leaks.
     */
    fun stopMonitoring() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
        isReceiverRegistered = false
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> PowerSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PowerSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PowerSource.WIRELESS
            else -> if (isCharging) PowerSource.AC else PowerSource.NONE
        }

        // On Android 9.0+ (API 28), BatteryManager provides computeChargeTimeRemaining()
        var chargeRemainingMs = -1L
        if (isCharging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                chargeRemainingMs = batteryManager?.computeChargeTimeRemaining() ?: -1L
            } catch (_: Exception) {
                chargeRemainingMs = -1L
            }
        }

        val tempCelsius = tempTenths / 10f

        _batteryState.value = BatteryState(
            level = if (level >= 0) level else 50,
            scale = if (scale > 0) scale else 100,
            isCharging = isCharging,
            isFull = isFull,
            powerSource = powerSource,
            chargeTimeRemainingMillis = chargeRemainingMs,
            temperatureCelsius = tempCelsius,
            health = health,
            timestamp = System.currentTimeMillis()
        )
    }
}
