package com.dynamicisland.app.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import android.os.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.features.battery.BatteryState
import com.dynamicisland.app.features.battery.CollapsedBatteryView
import com.dynamicisland.app.features.battery.ExpandedBatteryView
import com.dynamicisland.app.features.music.CollapsedMusicView
import com.dynamicisland.app.features.music.ExpandedMusicView
import com.dynamicisland.app.features.music.MusicState
import com.dynamicisland.app.features.notifications.CompactNotificationBannerView
import com.dynamicisland.app.features.notifications.IslandNotification
import com.dynamicisland.app.features.timer.CollapsedTimerView
import com.dynamicisland.app.features.timer.ExpandedTimerView
import com.dynamicisland.app.features.timer.TimerState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Root Composable for the Dynamic Island overlay.
 * Handles:
 * - Spring animation between collapsed pill and expanded card
 * - Tap to expand / collapse
 * - Swipe UP to dismiss (animates out and stays hidden until next event)
 * - Swipe DOWN on collapsed pill to expand
 * - Window-bounded touch geometry (status bar and outside apps remain tappable)
 * - Auto-hide countdown touch restart
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IslandView(
    uiState: IslandUiState,
    batteryState: BatteryState,
    musicState: MusicState,
    notificationState: IslandNotification,
    timerState: TimerState,
    basePillWidthDp: Int,
    isBatteryEnabled: Boolean,
    isMusicEnabled: Boolean,
    isNotificationEnabled: Boolean,
    isTimerEnabled: Boolean,
    onExpandRequest: () -> Unit,
    onCollapseRequest: () -> Unit,
    onDismissRequest: () -> Unit = {},
    onTouchInteraction: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onNotificationClick: (IslandNotification) -> Unit = {},
    isGlassStyleEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }

    // Spring animation spec: smooth and slightly bouncy
    val springSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Determine the active mode based on priorities:
    // First-Time Intro > Notification (compact pill) > Battery (brief alert) > Timer (while running) > Music (while playing/grace) > Idle
    val effectiveMode = when {
        uiState.activeMode == ActiveMode.FIRST_TIME_INTRO -> ActiveMode.FIRST_TIME_INTRO
        isNotificationEnabled && notificationState.isNotEmpty -> ActiveMode.NOTIFICATION
        isBatteryEnabled && uiState.activeMode == ActiveMode.BATTERY -> ActiveMode.BATTERY
        isTimerEnabled && timerState.isActive -> ActiveMode.TIMER
        isMusicEnabled && (musicState.isPlaying || (musicState.hasTrack && musicState.isDisplayActive)) -> ActiveMode.MUSIC
        uiState.activeMode != ActiveMode.IDLE -> uiState.activeMode
        else -> ActiveMode.IDLE
    }

    val isExpanded = uiState.isExpanded

    // Calculate dynamic target width, height, and corner radius
    val targetWidth = if (isExpanded) {
        340.dp
    } else {
        when (effectiveMode) {
            ActiveMode.FIRST_TIME_INTRO -> 350.dp
            ActiveMode.BATTERY -> (basePillWidthDp + 6).dp
            ActiveMode.MUSIC -> 150.dp // Item 6: Pill width about 150dp
            ActiveMode.NOTIFICATION -> 340.dp // Item 2: Compact pill max width 340dp
            ActiveMode.TIMER -> (basePillWidthDp + 16).dp
            ActiveMode.IDLE -> basePillWidthDp.dp
        }
    }

    val targetHeight = if (isExpanded) {
        when (effectiveMode) {
            ActiveMode.BATTERY -> 146.dp
            ActiveMode.MUSIC -> 188.dp // Item 7: Modern layout with album art, slider, controls
            ActiveMode.NOTIFICATION -> 60.dp // Item 2: Compact pill
            ActiveMode.TIMER -> 150.dp
            ActiveMode.IDLE -> 110.dp
            ActiveMode.FIRST_TIME_INTRO -> 44.dp
        }
    } else {
        when (effectiveMode) {
            ActiveMode.FIRST_TIME_INTRO -> 44.dp
            ActiveMode.NOTIFICATION -> 60.dp // Item 2: Compact pill
            else -> 36.dp // Item 6: Keep height at 36dp
        }
    }

    val targetCornerRadius = if (isExpanded || effectiveMode == ActiveMode.NOTIFICATION) 28.dp else 18.dp

    // Animate dimensions using bouncy spring physics
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = springSpec,
        label = "IslandWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = springSpec,
        label = "IslandHeight"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = springSpec,
        label = "IslandRadius"
    )

    val interactionSource = remember { MutableInteractionSource() }

    // Sized strictly to the card/pill.
    // No full-screen scrim: outside touches pass cleanly to underlying apps and status bar icons.
    Box(
        modifier = modifier.wrapContentSize(Alignment.TopCenter),
        contentAlignment = Alignment.TopCenter
    ) {
        // Card / Pill Background & Border styling (Item 2: Glass style behind setting)
        val cardBackgroundColor = if (isExpanded && isGlassStyleEnabled) {
            Color.Black.copy(alpha = 0.70f)
        } else {
            Color.Black
        }

        val cardBorderColor = if (isExpanded && isGlassStyleEnabled) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.08f)
        }

        val cardElevation = if (isExpanded) {
            if (isGlassStyleEnabled) 16.dp else 12.dp
        } else {
            4.dp
        }

        // The Island Pill / Card
        Box(
            modifier = Modifier
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .width(animatedWidth)
                .height(animatedHeight)
                .shadow(
                    elevation = cardElevation,
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .clip(RoundedCornerShape(animatedCornerRadius))
                .background(cardBackgroundColor)
                .border(
                    width = 1.dp,
                    color = cardBorderColor,
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                // Gesture handling: Swipe UP to dismiss, Swipe DOWN on collapsed pill to expand
                .pointerInput(isExpanded) {
                    detectVerticalDragGestures(
                        onDragStart = { _ ->
                            onTouchInteraction()
                        },
                        onDragEnd = {
                            onTouchInteraction()
                            val currentOffset = dragOffsetY.value
                            if (currentOffset < -25f) {
                                // Item 5: Swipe UP to dismiss
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = -100f,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                                    )
                                    onDismissRequest()
                                    dragOffsetY.snapTo(0f)
                                }
                            } else if (!isExpanded && currentOffset > 20f) {
                                // Item 5: Swipe DOWN on collapsed pill to expand
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                                val now = System.currentTimeMillis()
                                if (now - lastTapTimestamp >= 400L) {
                                    lastTapTimestamp = now
                                    onExpandRequest()
                                }
                            } else {
                                // Spring snap-back
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            onTouchInteraction()
                            val current = dragOffsetY.value
                            val damped = dragAmount * 0.45f
                            val newOffset = (current + damped).coerceIn(-100f, 50f)
                            coroutineScope.launch {
                                dragOffsetY.snapTo(newOffset)
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTimestamp < 400L) {
                        return@clickable // Disambiguate and prevent double expand/collapse
                    }
                    lastTapTimestamp = now
                    onTouchInteraction()
                    if (effectiveMode == ActiveMode.NOTIFICATION) {
                        onNotificationClick(notificationState)
                    } else if (isExpanded) {
                        onCollapseRequest()
                    } else {
                        onExpandRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Android 12+ background blur layer for glass card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isExpanded && isGlassStyleEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(animatedCornerRadius))
                        .graphicsLayer {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                20f,
                                20f,
                                android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                        .background(Color.Black.copy(alpha = 0.70f))
                )
            }
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "IslandContentTransition"
            ) { expanded ->
                if (expanded) {
                    // EXPANDED VIEW
                    when (effectiveMode) {
                        ActiveMode.BATTERY -> {
                            ExpandedBatteryView(state = batteryState)
                        }
                        ActiveMode.MUSIC -> {
                            ExpandedMusicView(
                                state = musicState,
                                onPlayPauseClick = onPlayPauseClick,
                                onPreviousClick = onPreviousClick,
                                onNextClick = onNextClick,
                                onSeekTo = onSeekTo
                            )
                        }
                        ActiveMode.NOTIFICATION -> {
                            CompactNotificationBannerView(
                                notification = notificationState,
                                onClick = { onNotificationClick(notificationState) }
                            )
                        }
                        ActiveMode.TIMER -> {
                            ExpandedTimerView(state = timerState)
                        }
                        ActiveMode.FIRST_TIME_INTRO -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD71921))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dynamic Island Active",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        ActiveMode.IDLE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Dynamic Island",
                                        tint = Color(0xFFD71921),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Dynamic Island",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ready • Tap to collapse",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    // COLLAPSED VIEW
                    when (effectiveMode) {
                        ActiveMode.BATTERY -> {
                            CollapsedBatteryView(state = batteryState)
                        }
                        ActiveMode.MUSIC -> {
                            CollapsedMusicView(state = musicState)
                        }
                        ActiveMode.NOTIFICATION -> {
                            CompactNotificationBannerView(
                                notification = notificationState,
                                onClick = { onNotificationClick(notificationState) }
                            )
                        }
                        ActiveMode.TIMER -> {
                            CollapsedTimerView(state = timerState)
                        }
                        ActiveMode.FIRST_TIME_INTRO -> {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD71921))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dynamic Island",
                                    color = Color(0xFFE5E5EA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        ActiveMode.IDLE -> {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(3.5.dp))
                                        .background(Color(0xFFD71921))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Island",
                                    color = Color(0xFFE5E5EA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
