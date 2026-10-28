package com.dynamicisland.app

import com.dynamicisland.app.features.music.MusicState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicStateTest {

    @Test
    fun testHasTrack() {
        val empty = MusicState()
        assertFalse(empty.hasTrack)

        val track = MusicState(title = "Starboy", artist = "The Weeknd")
        assertTrue(track.hasTrack)
    }

    @Test
    fun testTimeFormatting() {
        val state = MusicState()
        assertEquals("0:00", state.formatTime(0L))
        assertEquals("0:05", state.formatTime(5000L))
        assertEquals("3:45", state.formatTime(225000L))
        assertEquals("10:02", state.formatTime(602000L))
    }

    @Test
    fun testProgressFraction() {
        val state = MusicState(
            durationMs = 200000L,
            positionMs = 100000L
        )
        assertEquals(0.5f, state.progressFraction, 0.001f)

        val zeroDuration = MusicState(durationMs = 0L, positionMs = 50000L)
        assertEquals(0f, zeroDuration.progressFraction, 0.001f)

        // Clamping overflow
        val overflow = MusicState(durationMs = 100000L, positionMs = 150000L)
        assertEquals(1.0f, overflow.progressFraction, 0.001f)
    }

    @Test
    fun testDisplayActiveState() {
        val defaultState = MusicState()
        assertFalse(defaultState.isDisplayActive)

        val playingState = MusicState(title = "Song", isPlaying = true, isDisplayActive = true)
        assertTrue(playingState.isDisplayActive)
        assertTrue(playingState.isPlaying)

        val pausedGraceState = MusicState(title = "Song", isPlaying = false, isDisplayActive = true)
        assertTrue(pausedGraceState.isDisplayActive)
        assertFalse(pausedGraceState.isPlaying)

        val pausedExpiredState = MusicState(title = "Song", isPlaying = false, isDisplayActive = false)
        assertFalse(pausedExpiredState.isDisplayActive)
    }
}
