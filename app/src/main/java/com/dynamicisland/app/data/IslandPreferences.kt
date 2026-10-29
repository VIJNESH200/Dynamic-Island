package com.dynamicisland.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent preferences for the Dynamic Island app.
 * Provides StateFlow streams so the UI and overlay service can react
 * immediately to configuration changes.
 */
class IslandPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isIslandEnabled = MutableStateFlow(prefs.getBoolean(KEY_ISLAND_ENABLED, true))
    val isIslandEnabled: StateFlow<Boolean> = _isIslandEnabled.asStateFlow()

    private val _isBatteryFeatureEnabled = MutableStateFlow(prefs.getBoolean(KEY_BATTERY_ENABLED, true))
    val isBatteryFeatureEnabled: StateFlow<Boolean> = _isBatteryFeatureEnabled.asStateFlow()

    private val _isMusicFeatureEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC_ENABLED, true))
    val isMusicFeatureEnabled: StateFlow<Boolean> = _isMusicFeatureEnabled.asStateFlow()

    private val _isNotificationFeatureEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true))
    val isNotificationFeatureEnabled: StateFlow<Boolean> = _isNotificationFeatureEnabled.asStateFlow()

    private val _isTimerFeatureEnabled = MutableStateFlow(prefs.getBoolean(KEY_TIMER_ENABLED, true))
    val isTimerFeatureEnabled: StateFlow<Boolean> = _isTimerFeatureEnabled.asStateFlow()

    private val _hasShownFirstTimeIntro = MutableStateFlow(prefs.getBoolean(KEY_FIRST_TIME_INTRO, false))
    val hasShownFirstTimeIntro: StateFlow<Boolean> = _hasShownFirstTimeIntro.asStateFlow()

    // Extra vertical offset in DP added on top of the calculated status bar height + 4dp gap
    private val _verticalOffsetDp = MutableStateFlow(prefs.getInt(KEY_VERTICAL_OFFSET, DEFAULT_OFFSET_DP))
    val verticalOffsetDp: StateFlow<Int> = _verticalOffsetDp.asStateFlow()

    // Pill width in collapsed state (approx 120-140dp)
    private val _pillWidthDp = MutableStateFlow(prefs.getInt(KEY_PILL_WIDTH, DEFAULT_PILL_WIDTH_DP))
    val pillWidthDp: StateFlow<Int> = _pillWidthDp.asStateFlow()

    // Auto-hide timeout in seconds (3, 5, 10, or -1 for Never)
    private val _autoHideSeconds = MutableStateFlow(prefs.getInt(KEY_AUTO_HIDE_SECONDS, DEFAULT_AUTO_HIDE_SECONDS))
    val autoHideSeconds: StateFlow<Int> = _autoHideSeconds.asStateFlow()

    fun setIslandEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ISLAND_ENABLED, enabled).apply()
        _isIslandEnabled.value = enabled
    }

    fun setBatteryFeatureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BATTERY_ENABLED, enabled).apply()
        _isBatteryFeatureEnabled.value = enabled
    }

    fun setMusicFeatureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
        _isMusicFeatureEnabled.value = enabled
    }

    fun setNotificationFeatureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        _isNotificationFeatureEnabled.value = enabled
    }

    fun setTimerFeatureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIMER_ENABLED, enabled).apply()
        _isTimerFeatureEnabled.value = enabled
    }

    fun setHasShownFirstTimeIntro(shown: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_TIME_INTRO, shown).apply()
        _hasShownFirstTimeIntro.value = shown
    }

    fun setVerticalOffsetDp(offset: Int) {
        prefs.edit().putInt(KEY_VERTICAL_OFFSET, offset).apply()
        _verticalOffsetDp.value = offset
    }

    fun setPillWidthDp(width: Int) {
        prefs.edit().putInt(KEY_PILL_WIDTH, width).apply()
        _pillWidthDp.value = width
    }

    fun setAutoHideSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_HIDE_SECONDS, seconds).apply()
        _autoHideSeconds.value = seconds
    }

    companion object {
        private const val PREFS_NAME = "dynamic_island_prefs"
        private const val KEY_ISLAND_ENABLED = "island_enabled"
        private const val KEY_BATTERY_ENABLED = "battery_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_TIMER_ENABLED = "timer_enabled"
        private const val KEY_FIRST_TIME_INTRO = "has_shown_first_time_intro"
        private const val KEY_VERTICAL_OFFSET = "vertical_offset_dp"
        private const val KEY_PILL_WIDTH = "pill_width_dp"
        private const val KEY_AUTO_HIDE_SECONDS = "auto_hide_seconds"

        // Default extra offset: 0dp (pill sits cleanly 4dp below the system status bar)
        const val DEFAULT_OFFSET_DP = 0
        const val DEFAULT_PILL_WIDTH_DP = 126
        const val DEFAULT_AUTO_HIDE_SECONDS = 5

        @Volatile
        private var instance: IslandPreferences? = null

        fun getInstance(context: Context): IslandPreferences {
            return instance ?: synchronized(this) {
                instance ?: IslandPreferences(context).also { instance = it }
            }
        }
    }
}
