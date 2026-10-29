package com.dynamicisland.app

import com.dynamicisland.app.features.timer.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    @Test
    fun testDefaultTimerState() {
        val timer = TimerState()
        assertFalse(timer.isRunning)
        assertFalse(timer.isActive)
        assertEquals("Timer", timer.label)
        assertEquals("", timer.remainingTimeText)
    }

    @Test
    fun testActiveTimerState() {
        val timer = TimerState(
            isRunning = true,
            label = "Eggs",
            remainingTimeText = "04:30"
        )
        assertTrue(timer.isRunning)
        assertTrue(timer.isActive)
        assertEquals("Eggs", timer.label)
        assertEquals("04:30", timer.remainingTimeText)
    }

    @Test
    fun testPausedTimerState() {
        val timer = TimerState(
            isRunning = false,
            remainingTimeText = "02:15"
        )
        assertFalse(timer.isRunning)
        assertFalse(timer.isActive)
    }
}
