package com.dynamicisland.app

import com.dynamicisland.app.overlay.ActiveMode
import com.dynamicisland.app.overlay.ExpansionState
import com.dynamicisland.app.overlay.IslandUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandStateTest {

    @Test
    fun testDefaultState() {
        val state = IslandUiState()
        assertEquals(ExpansionState.COLLAPSED, state.expansionState)
        assertEquals(ActiveMode.IDLE, state.activeMode)
        assertTrue(state.isCollapsed)
        assertFalse(state.isExpanded)
    }

    @Test
    fun testExpandedState() {
        val state = IslandUiState(
            expansionState = ExpansionState.EXPANDED,
            activeMode = ActiveMode.BATTERY
        )
        assertTrue(state.isExpanded)
        assertFalse(state.isCollapsed)
        assertEquals(ActiveMode.BATTERY, state.activeMode)
    }
}
