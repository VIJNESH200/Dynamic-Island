package com.dynamicisland.app.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.features.battery.BatteryManagerHelper
import com.dynamicisland.app.features.music.MusicControllerHelper
import com.dynamicisland.app.features.notifications.IslandNotification
import com.dynamicisland.app.features.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Manages the WindowManager overlay for Dynamic Island.
 *
 * Touch Architecture:
 * - Window size strictly matches the pill (~126-150dp) or expanded card (340dp).
 * - Window Y position starts cleanly below the real status bar height (with 4dp gap).
 * - Status bar icons (clock, battery, Wi-Fi, notifications) remain completely uncovered
 *   and tappable in BOTH collapsed and expanded states.
 * - Outside touches pass directly through to underlying apps; outside taps on the expanded
 *   card trigger MotionEvent.ACTION_OUTSIDE to collapse the card cleanly without blocking touches.
 *
 * Lifecycle:
 * - On-demand: removed from the screen when idle; shown only during active triggers
 *   (charger plug/unplug, music playing + 6s grace, notification arrival, timer running).
 * - Auto-hide: Disappears after timeout (default 5s) unless touched.
 * - Swipe to dismiss: Swipe UP dismisses and keeps hidden until next event.
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

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Trigger states
    private val _isChargerTriggerActive = MutableStateFlow(false)
    private val _isMusicTriggerActive = MutableStateFlow(false)
    private val _isFirstTimeIntroActive = MutableStateFlow(false)
    private val _isManualPreviewActive = MutableStateFlow(false)
    private val _isDismissed = MutableStateFlow(false)
    val isDismissed: StateFlow<Boolean> = _isDismissed.asStateFlow()

    private var lastMusicTrackId: String? = null
    private var wasMusicPlaying: Boolean = false
    private var isAnimating: Boolean = false

    private var notificationDismissJob: Job? = null
    private var powerAlertJob: Job? = null
    private var autoHideJob: Job? = null

    companion object {
        // Small 4dp gap below the status bar so the pill never overlaps clock, icons or camera hole
        const val STATUS_BAR_GAP_DP = 4
    }

    init {
        // Observe preference changes that affect placement or visibility
        overlayScope.launch {
            preferences.verticalOffsetDp.collect {
                if (isOverlayAttached && composeView != null) {
                    val params = if (_uiState.value.isExpanded) createExpandedParams() else createCollapsedParams()
                    try {
                        windowManager.updateViewLayout(composeView, params)
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe auto-hide setting changes
        overlayScope.launch {
            preferences.autoHideSeconds.collect {
                resetAutoHideTimer()
            }
        }

        // Observe glass style setting changes
        overlayScope.launch {
            preferences.isGlassStyleEnabled.collect {
                if (isOverlayAttached && composeView != null) {
                    val params = if (_uiState.value.isExpanded) createExpandedParams() else createCollapsedParams()
                    try {
                        windowManager.updateViewLayout(composeView, params)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Resets the auto-hide timer countdown.
     * Any touch on the island or state change restarts the countdown from zero.
     */
    fun resetAutoHideTimer() {
        autoHideJob?.cancel()
        val timeoutSeconds = preferences.autoHideSeconds.value
        if (timeoutSeconds <= 0) {
            return // Never auto-hide
        }

        autoHideJob = overlayScope.launch {
            delay(timeoutSeconds * 1000L)
            if (isOverlayAttached) {
                if (_uiState.value.isExpanded) {
                    collapse()
                } else if (!_isMusicTriggerActive.value && !_timerState.value.isActive) {
                    hide()
                }
            }
        }
    }

    /**
     * Swiped UP to dismiss:
     * Hides the island immediately and keeps it hidden until the next trigger event.
     */
    fun dismiss() {
        _isDismissed.value = true
        autoHideJob?.cancel()
        hide()
    }

    /**
     * Calculates the real system status bar height in DP at runtime.
     * Uses WindowMetrics / WindowInsets on Android 11+ (API 30+) or the system "status_bar_height" resource.
     */
    fun getStatusBarHeightDp(): Int {
        var statusBarHeightPx = 0

        // 1. Modern Android 11+ (API 30+) WindowInsets query
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val windowMetrics = windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.statusBars()
                )
                if (insets.top > 0) {
                    statusBarHeightPx = insets.top
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to query status bar insets from WindowMetrics: ${e.message}")
            }
        }

        // 2. Fallback / earlier Android versions: query system status_bar_height resource
        if (statusBarHeightPx <= 0) {
            try {
                val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    statusBarHeightPx = context.resources.getDimensionPixelSize(resourceId)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to query status_bar_height resource: ${e.message}")
            }
        }

        val density = context.resources.displayMetrics.density
        return if (density > 0f && statusBarHeightPx > 0) {
            (statusBarHeightPx / density).roundToInt()
        } else {
            24
        }
    }

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

                // Intercept touches outside the card window bounds using FLAG_WATCH_OUTSIDE_TOUCH
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        if (!isAnimating && _uiState.value.isExpanded) {
                            collapse()
                        }
                        return@setOnTouchListener false
                    }
                    false
                }

                setContent {
                    val currentUiState by _uiState.collectAsState()
                    val batteryState by batteryHelper.batteryState.collectAsState()
                    val musicState by musicHelper.musicState.collectAsState()
                    val currentNotification by _notificationState.collectAsState()
                    val currentTimer by _timerState.collectAsState()

                    val pillWidth by preferences.pillWidthDp.collectAsState()
                    val isBatteryEnabled by preferences.isBatteryFeatureEnabled.collectAsState()
                    val isMusicEnabled by preferences.isMusicFeatureEnabled.collectAsState()
                    val isNotificationEnabled by preferences.isNotificationFeatureEnabled.collectAsState()
                    val isTimerEnabled by preferences.isTimerFeatureEnabled.collectAsState()
                    val isGlassEnabled by preferences.isGlassStyleEnabled.collectAsState()

                    IslandView(
                        uiState = currentUiState,
                        batteryState = batteryState,
                        musicState = musicState,
                        notificationState = currentNotification,
                        timerState = currentTimer,
                        basePillWidthDp = pillWidth,
                        isBatteryEnabled = isBatteryEnabled,
                        isMusicEnabled = isMusicEnabled,
                        isNotificationEnabled = isNotificationEnabled,
                        isTimerEnabled = isTimerEnabled,
                        isGlassStyleEnabled = isGlassEnabled,
                        onExpandRequest = {
                            resetAutoHideTimer()
                            expand()
                        },
                        onCollapseRequest = {
                            resetAutoHideTimer()
                            collapse()
                        },
                        onDismissRequest = {
                            dismiss()
                        },
                        onTouchInteraction = {
                            resetAutoHideTimer()
                        },
                        onSeekTo = { posMs ->
                            resetAutoHideTimer()
                            musicHelper.seekTo(posMs)
                        },
                        onPlayPauseClick = {
                            resetAutoHideTimer()
                            musicHelper.togglePlayPause()
                        },
                        onPreviousClick = {
                            resetAutoHideTimer()
                            musicHelper.skipToPrevious()
                        },
                        onNextClick = {
                            resetAutoHideTimer()
                            musicHelper.skipToNext()
                        },
                        onNotificationClick = { notif ->
                            handleNotificationClick(notif)
                        }
                    )
                }
            }

            composeView = view

            // Check if first-time intro pop-up needs to play
            if (!preferences.hasShownFirstTimeIntro.value) {
                val introParams = createFirstTimeIntroParams()
                windowManager.addView(view, introParams)
                isOverlayAttached = true
                owner.onStart()
                owner.onResume()
                playFirstTimeIntroAnimation()
            } else {
                val params = if (_uiState.value.isExpanded) createExpandedParams() else createCollapsedParams()
                windowManager.addView(view, params)
                isOverlayAttached = true
                owner.onStart()
                owner.onResume()
            }

            resetAutoHideTimer()
            Log.d(tag, "Dynamic Island overlay attached successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to add Dynamic Island overlay: ${e.message}", e)
        }
    }

    /**
     * The status bar cannot be hidden by a third-party overlay.
     * Android security policies prohibit non-system applications from hiding, modifying,
     * or suppressing the system status bar without root or system signature permissions.
     * Do not attempt to hide it or invoke hidden/non-public APIs.
     *
     * Instead, on the first appearance, this method plays a brief 1.5-second presentation
     * expansion across the top status bar area, then collapses to sit cleanly below it.
     */
    private fun playFirstTimeIntroAnimation() {
        _isFirstTimeIntroActive.value = true
        _uiState.value = _uiState.value.copy(
            activeMode = ActiveMode.FIRST_TIME_INTRO,
            expansionState = ExpansionState.COLLAPSED
        )

        overlayScope.launch {
            delay(1500)
            preferences.setHasShownFirstTimeIntro(true)
            _isFirstTimeIntroActive.value = false
            _uiState.value = _uiState.value.copy(
                activeMode = ActiveMode.IDLE,
                expansionState = ExpansionState.COLLAPSED
            )

            if (composeView != null && isOverlayAttached) {
                try {
                    windowManager.updateViewLayout(composeView, createCollapsedParams())
                } catch (_: Exception) {}
            }
            updateOverlayVisibility()
        }
    }

    /**
     * Hides and removes the overlay window from the WindowManager.
     */
    fun hide() {
        autoHideJob?.cancel()
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
            Log.d(tag, "Dynamic Island overlay removed from screen")
        } catch (e: Exception) {
            Log.e(tag, "Error removing Dynamic Island overlay: ${e.message}", e)
        }
    }

    /**
     * Expands the island smoothly into a card.
     * The WindowManager window is resized to the expanded size BEFORE the expand animation starts.
     * The collapse path cannot run while the expand animation is in progress.
     */
    fun expand() {
        if (!isOverlayAttached) return
        if (_uiState.value.isExpanded || isAnimating) return

        Log.d("DI_UI", "DynamicIslandOverlay.expand() called, current=${_uiState.value.expansionState}")
        isAnimating = true

        // Resize WindowManager window to expanded size BEFORE starting expand animation
        updateWindowLayoutParams(isExpanded = true)

        _uiState.value = _uiState.value.copy(expansionState = ExpansionState.EXPANDED)
        resetAutoHideTimer()

        overlayScope.launch {
            delay(350)
            isAnimating = false
        }
    }

    /**
     * Collapses the island back to the small pill.
     * Guarded by isAnimating so it cannot interrupt an expand animation in progress.
     */
    fun collapse() {
        if (!isOverlayAttached) return
        if (!_uiState.value.isExpanded || isAnimating) return

        Log.d("DI_UI", "DynamicIslandOverlay.collapse() called, current=${_uiState.value.expansionState}")
        isAnimating = true

        _uiState.value = _uiState.value.copy(expansionState = ExpansionState.COLLAPSED)

        overlayScope.launch {
            delay(320)
            if (_uiState.value.isCollapsed && isOverlayAttached) {
                updateWindowLayoutParams(isExpanded = false)
            }
            isAnimating = false
        }
        resetAutoHideTimer()
    }

    /**
     * Updates overlay visibility based on whether any trigger is currently active.
     * When nothing is active or when dismissed, the overlay is removed from the screen.
     */
    fun updateOverlayVisibility() {
        if (!android.provider.Settings.canDrawOverlays(context) || !preferences.isIslandEnabled.value) {
            hide()
            return
        }

        if (_isDismissed.value) {
            hide()
            return
        }

        val batteryActive = _isChargerTriggerActive.value && preferences.isBatteryFeatureEnabled.value
        val musicActive = _isMusicTriggerActive.value && preferences.isMusicFeatureEnabled.value
        val notifActive = _notificationState.value.isNotEmpty && preferences.isNotificationFeatureEnabled.value
        val timerActive = _timerState.value.isActive && preferences.isTimerFeatureEnabled.value
        val introActive = _isFirstTimeIntroActive.value
        val previewActive = _isManualPreviewActive.value

        val shouldBeVisible = batteryActive || musicActive || notifActive || timerActive || introActive || previewActive

        if (shouldBeVisible) {
            val nextMode = when {
                introActive -> ActiveMode.FIRST_TIME_INTRO
                notifActive -> ActiveMode.NOTIFICATION
                batteryActive -> ActiveMode.BATTERY
                timerActive -> ActiveMode.TIMER
                musicActive -> ActiveMode.MUSIC
                else -> ActiveMode.IDLE
            }

            if (_uiState.value.activeMode != nextMode) {
                _uiState.value = _uiState.value.copy(activeMode = nextMode)
            }

            if (!isOverlayAttached) {
                show()
            }
            resetAutoHideTimer()
        } else {
            if (isOverlayAttached) {
                hide()
            }
        }
    }

    /**
     * Triggers the charger connected or disconnected alert:
     * Shows the collapsed pill for 4 seconds, then hides if no other trigger is active.
     */
    fun triggerChargerAlert() {
        _isDismissed.value = false
        powerAlertJob?.cancel()
        _isChargerTriggerActive.value = true
        _uiState.value = _uiState.value.copy(
            activeMode = ActiveMode.BATTERY,
            expansionState = ExpansionState.COLLAPSED
        )
        updateOverlayVisibility()
        resetAutoHideTimer()

        powerAlertJob = overlayScope.launch {
            delay(4000)
            _isChargerTriggerActive.value = false
            updateOverlayVisibility()
        }
    }

    /**
     * Updates active music playback trigger:
     * Stays visible while playing, hides 6 seconds after pause.
     * Does NOT auto-expand: always keeps collapsed pill until tapped.
     */
    fun setMusicActive(isActive: Boolean, trackId: String = "") {
        val isNewTrack = trackId.isNotBlank() && trackId != lastMusicTrackId
        if (isActive && (!_isMusicTriggerActive.value || isNewTrack || !wasMusicPlaying)) {
            // New playback session or new track -> restore from dismissed
            _isDismissed.value = false
        }
        if (trackId.isNotBlank()) {
            lastMusicTrackId = trackId
        }
        wasMusicPlaying = isActive
        _isMusicTriggerActive.value = isActive
        if (isActive) {
            if (_uiState.value.activeMode != ActiveMode.NOTIFICATION &&
                _uiState.value.activeMode != ActiveMode.BATTERY &&
                _uiState.value.activeMode != ActiveMode.FIRST_TIME_INTRO
            ) {
                if (_uiState.value.activeMode != ActiveMode.MUSIC) {
                    _uiState.value = _uiState.value.copy(
                        activeMode = ActiveMode.MUSIC,
                        expansionState = ExpansionState.COLLAPSED
                    )
                }
            }
        }
        updateOverlayVisibility()
    }

    /**
     * Handles tap on an incoming notification banner:
     * - First try notification.contentIntent.send().
     * - If that fails or is null, open the app with packageManager.getLaunchIntentForPackage(packageName) with FLAG_ACTIVITY_NEW_TASK.
     * - If neither works, show a toast: "Can't open this app".
     * - Dismiss the island after opening the app.
     * - Log each tap with tag "DI_NOTIF_TAP" and include only the package name.
     * - Fallback to Gallery or Files only applies to recording notifications from known recorder packages.
     */
    fun handleNotificationClick(notification: IslandNotification) {
        val packageName = notification.packageName
        Log.d("DI_NOTIF_TAP", packageName)

        notificationDismissJob?.cancel()
        _notificationState.value = IslandNotification()
        dismiss() // Dismiss the island after opening the app

        var appOpened = false

        // 1. First try notification.contentIntent.send()
        val pendingIntent = notification.contentIntent
        if (pendingIntent != null) {
            try {
                pendingIntent.send()
                appOpened = true
            } catch (_: Exception) {
                appOpened = false
            }
        }

        // 2. Fallback to Gallery or Files only for known recorder packages
        if (!appOpened) {
            val pkgLower = packageName.lowercase()
            val isRecorder = pkgLower.contains("screenrecord") || pkgLower.contains("recorder")
            if (isRecorder) {
                try {
                    val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                        type = "video/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    if (videoIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(videoIntent)
                        appOpened = true
                    }
                } catch (_: Exception) {}

                if (!appOpened) {
                    try {
                        val filesIntent = Intent(Intent.ACTION_VIEW).apply {
                            type = "*/*"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (filesIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(filesIntent)
                            appOpened = true
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 3. Fallback: open the app with packageManager.getLaunchIntentForPackage(packageName)
        if (!appOpened && packageName.isNotBlank()) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    appOpened = true
                }
            } catch (_: Exception) {
                appOpened = false
            }
        }

        // 4. If neither works, show toast: "Can't open this app"
        if (!appOpened) {
            Toast.makeText(context, "Can't open this app", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Displays an incoming notification alert on the island briefly (~5s) then hides.
     */
    fun postNotificationAlert(notification: IslandNotification) {
        _isDismissed.value = false
        notificationDismissJob?.cancel()
        _notificationState.value = notification
        _uiState.value = _uiState.value.copy(
            activeMode = ActiveMode.NOTIFICATION,
            expansionState = ExpansionState.COLLAPSED
        )
        updateOverlayVisibility()
        resetAutoHideTimer()

        notificationDismissJob = overlayScope.launch {
            delay(5000)
            _notificationState.value = IslandNotification()
            updateOverlayVisibility()
        }
    }

    /**
     * Updates timer countdown state. Stays visible while the timer runs.
     */
    fun updateTimer(timer: TimerState) {
        if (timer.isActive && !_timerState.value.isActive) {
            _isDismissed.value = false
        }
        _timerState.value = timer
        if (timer.isActive) {
            if (_uiState.value.activeMode != ActiveMode.NOTIFICATION &&
                _uiState.value.activeMode != ActiveMode.BATTERY &&
                _uiState.value.activeMode != ActiveMode.FIRST_TIME_INTRO
            ) {
                _uiState.value = _uiState.value.copy(
                    activeMode = ActiveMode.TIMER,
                    expansionState = ExpansionState.COLLAPSED
                )
            }
        }
        updateOverlayVisibility()
    }

    /**
     * Manual preview triggered from UI screen for testing.
     */
    fun triggerManualPreview() {
        _isDismissed.value = false
        _isManualPreviewActive.value = true
        updateOverlayVisibility()
        resetAutoHideTimer()
        overlayScope.launch {
            delay(5000)
            _isManualPreviewActive.value = false
            updateOverlayVisibility()
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
        val density = context.resources.displayMetrics.density
        val statusBarHeight = getStatusBarHeightDp()
        val extraOffset = preferences.verticalOffsetDp.value
        val topOffsetPx = ((statusBarHeight + STATUS_BAR_GAP_DP + extraOffset) * density).roundToInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = topOffsetPx
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            params.setBlurBehindRadius(0)
        }

        return params
    }

    private fun createExpandedParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val statusBarHeight = getStatusBarHeightDp()
        val extraOffset = preferences.verticalOffsetDp.value
        val topOffsetPx = ((statusBarHeight + STATUS_BAR_GAP_DP + extraOffset) * density).roundToInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = topOffsetPx
        }

        // On Android 12+ (API 31+), blur what is behind the card window only when expanded and glass style enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && preferences.isGlassStyleEnabled.value) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.setBlurBehindRadius(40)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            params.setBlurBehindRadius(0)
        }

        return params
    }

    /**
     * Window layout params for the brief 1.5s first-time intro pop-up.
     * Covers from y = 0 across status bar area, then collapses below it.
     */
    private fun createFirstTimeIntroParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
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
}
