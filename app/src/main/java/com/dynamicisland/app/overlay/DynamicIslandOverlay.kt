package com.dynamicisland.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.features.battery.BatteryManagerHelper
import com.dynamicisland.app.features.music.MusicControllerHelper
import com.dynamicisland.app.features.notifications.IslandNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the WindowManager overlay for Dynamic Island.
 * Attaches ComposeView with OverlayLifecycleOwner, handles WindowManager LayoutParams,
 * and coordinates expand/collapse window resizing.
 */
class DynamicIslandOverlay(
    private val context: Context,
    private val preferences: IslandPreferences,
    private val batteryHelper: BatteryManagerHelper,
    private val musicHelper: MusicControllerHelper
) {
    private val tag = "DynamicIslandOverlay"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayScope = CoroutineScope(Dispatchers.Main + Job())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isOverlayAttached = false

    private val _uiState = MutableStateFlow(IslandUiState())
    val uiState: StateFlow<IslandUiState> = _uiState.asStateFlow()

    private val _notificationState = MutableStateFlow(IslandNotification())
    val notificationState: StateFlow<IslandNotification> = _notificationState.asStateFlow()

    private var notificationDismissJob: Job? = null
    private var powerAlertJob: Job? = null

    /**
     * Shows the Dynamic Island overlay window on screen.
     */
    fun show() {
        if (isOverlayAttached) return

        try {
            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner
            owner.onCreate()

            val view = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                owner.attachToComposeView(this)
                setContent {
                    val currentUiState by _uiState.collectAsState()
                    val batteryState by batteryHelper.batteryState.collectAsState()
                    val musicState by musicHelper.musicState.collectAsState()
                    val currentNotification by _notificationState.collectAsState()

                    val verticalOffset by preferences.verticalOffsetDp.collectAsState()
                    val pillWidth by preferences.pillWidthDp.collectAsState()
                    val isBatteryEnabled by preferences.isBatteryFeatureEnabled.collectAsState()
                    val isMusicEnabled by preferences.isMusicFeatureEnabled.collectAsState()
                    val isNotificationEnabled by preferences.isNotificationFeatureEnabled.collectAsState()

                    IslandView(
                        uiState = currentUiState,
                        batteryState = batteryState,
                        musicState = musicState,
                        notificationState = currentNotification,
                        verticalOffsetDp = verticalOffset,
                        basePillWidthDp = pillWidth,
                        isBatteryEnabled = isBatteryEnabled,
                        isMusicEnabled = isMusicEnabled,
                        isNotificationEnabled = isNotificationEnabled,
                        onExpandRequest = { expand() },
                        onCollapseRequest = { collapse() },
                        onPlayPauseClick = { musicHelper.togglePlayPause() },
                        onPreviousClick = { musicHelper.skipToPrevious() },
                        onNextClick = { musicHelper.skipToNext() }
                    )
                }
            }

            composeView = view

            val params = createCollapsedParams()
            windowManager.addView(view, params)
            isOverlayAttached = true

            owner.onStart()
            owner.onResume()

            Log.d(tag, "Dynamic Island overlay attached successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to add Dynamic Island overlay: ${e.message}", e)
        }
    }

    /**
     * Hides and destroys the overlay window.
     */
    fun hide() {
        if (!isOverlayAttached) return

        try {
            lifecycleOwner?.onPause()
            lifecycleOwner?.onStop()
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null

            composeView?.let { view ->
                windowManager.removeViewImmediate(view)
            }
            composeView = null
            isOverlayAttached = false
            Log.d(tag, "Dynamic Island overlay removed successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error removing Dynamic Island overlay: ${e.message}", e)
        }
    }

    /**
     * Expands the island smoothly into a card.
     * Expands WindowManager bounds to MATCH_PARENT to intercept tap-outside.
     */
    fun expand() {
        if (!isOverlayAttached) return
        powerAlertJob?.cancel()
        _uiState.value = _uiState.value.copy(expansionState = ExpansionState.EXPANDED)
        updateWindowLayoutParams(isExpanded = true)
    }

    /**
     * Collapses the island back to the small pill.
     * Animates back, then shrinks WindowManager bounds to WRAP_CONTENT.
     */
    fun collapse() {
        if (!isOverlayAttached) return
        powerAlertJob?.cancel()
        _uiState.value = _uiState.value.copy(expansionState = ExpansionState.COLLAPSED)

        // Allow 320ms for the spring animation to smoothly settle before shrinking WindowManager frame
        overlayScope.launch {
            delay(320)
            if (_uiState.value.isCollapsed && isOverlayAttached) {
                updateWindowLayoutParams(isExpanded = false)
            }
        }
    }

    /**
     * Displays an incoming notification alert on the island briefly (~4.5s) then collapses.
     */
    fun postNotificationAlert(notification: IslandNotification) {
        _notificationState.value = notification
        notificationDismissJob?.cancel()
        notificationDismissJob = overlayScope.launch {
            delay(4500)
            _notificationState.value = IslandNotification()
        }
    }

    /**
     * Displays a power connected charging alert: expands the island for 4 seconds,
     * then collapses smoothly back to the pill.
     */
    fun showPowerConnectedAlert() {
        if (!isOverlayAttached) return
        expand()
        powerAlertJob?.cancel()
        powerAlertJob = overlayScope.launch {
            delay(4000)
            if (_uiState.value.isExpanded && isOverlayAttached) {
                collapse()
            }
        }
    }

    private fun updateWindowLayoutParams(isExpanded: Boolean) {
        val view = composeView ?: return
        try {
            val params = if (isExpanded) {
                createExpandedParams()
            } else {
                createCollapsedParams()
            }
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w(tag, "Failed to update overlay layout params: ${e.message}")
        }
    }

    private fun createCollapsedParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }
    }

    private fun createExpandedParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }
    }
}
