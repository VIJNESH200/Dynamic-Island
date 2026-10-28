package com.dynamicisland.app

import com.dynamicisland.app.features.battery.BatteryState
import com.dynamicisland.app.features.battery.PowerSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStateTest {

    @Test
    fun testPercentageCalculation() {
        val state = BatteryState(level = 85, scale = 100)
        assertEquals(85, state.percentage)
        assertEquals("85%", state.shortPercentageText)
    }

    @Test
    fun testClampedPercentage() {
        val overflowState = BatteryState(level = 150, scale = 100)
        assertEquals(100, overflowState.percentage)

        val zeroScaleState = BatteryState(level = 45, scale = 0)
        assertEquals(45, zeroScaleState.percentage)
    }

    @Test
    fun testFormatRemainingTimeWhenNotCharging() {
        val state = BatteryState(level = 50, isCharging = false)
        assertEquals("On battery", state.formatRemainingTime())
    }

    @Test
    fun testFormatRemainingTimeWhenFull() {
        val state = BatteryState(level = 100, isCharging = true, isFull = true)
        assertEquals("Fully Charged", state.formatRemainingTime())
    }

    @Test
    fun testFormatRemainingTimeWithComputedMillis() {
        // 45 minutes = 45 * 60 * 1000 = 2,700,000 ms
        val state = BatteryState(
            level = 60,
            isCharging = true,
            chargeTimeRemainingMillis = 2_700_000L
        )
        assertEquals("45m to full", state.formatRemainingTime())

        // 1 hour 15 minutes = 75 * 60 * 1000 = 4,500,000 ms
        val stateHours = BatteryState(
            level = 30,
            isCharging = true,
            chargeTimeRemainingMillis = 4_500_000L
        )
        assertEquals("1h 15m to full", stateHours.formatRemainingTime())
    }

    @Test
    fun testPowerSourceDescription() {
        assertEquals("AC Fast Charger", BatteryState(powerSource = PowerSource.AC).powerSourceDescription())
        assertEquals("USB Cable", BatteryState(powerSource = PowerSource.USB).powerSourceDescription())
        assertEquals("Wireless Dock", BatteryState(powerSource = PowerSource.WIRELESS).powerSourceDescription())
        assertEquals("Not Connected", BatteryState(powerSource = PowerSource.NONE).powerSourceDescription())
    }
}
