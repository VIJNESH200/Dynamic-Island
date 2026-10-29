package com.dynamicisland.app.overlay

/**
 * Expansion state of the Dynamic Island.
 */
enum class ExpansionState {
    COLLAPSED,
    EXPANDED
}

/**
 * Which feature is actively displayed on the island.
 */
enum class ActiveMode {
    IDLE,
    BATTERY,
    MUSIC,
    NOTIFICATION,
    TIMER,
    FIRST_TIME_INTRO
}

/**
 * Current visual configuration of the Dynamic Island.
 */
data class IslandUiState(
    val expansionState: ExpansionState = ExpansionState.COLLAPSED,
    val activeMode: ActiveMode = ActiveMode.IDLE,
    val isDragging: Boolean = false,
    val dragOffsetY: Float = 0f
) {
    val isExpanded: Boolean
        get() = expansionState == ExpansionState.EXPANDED

    val isCollapsed: Boolean
        get() = expansionState == ExpansionState.COLLAPSED
}
