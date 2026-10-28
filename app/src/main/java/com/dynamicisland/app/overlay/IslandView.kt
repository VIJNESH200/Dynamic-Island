package com.dynamicisland.app.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.dynamicisland.app.features.notifications.CollapsedNotificationView
import com.dynamicisland.app.features.notifications.ExpandedNotificationView
import com.dynamicisland.app.features.notifications.IslandNotification
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Root Composable for the Dynamic Island overlay.
 * Handles:
 * - Spring animation between collapsed pill and expanded card
 * - Tap to expand / collapse
 * - Tap outside to collapse
 * - Vertical drag with bouncy spring snap-back
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IslandView(
    uiState: IslandUiState,
    batteryState: BatteryState,
    musicState: MusicState,
    notificationState: IslandNotification,
    verticalOffsetDp: Int,
    basePillWidthDp: Int,
    isBatteryEnabled: Boolean,
    isMusicEnabled: Boolean,
    isNotificationEnabled: Boolean,
    onExpandRequest: () -> Unit,
    onCollapseRequest: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    // Spring animation spec: smooth and slightly bouncy as requested
    val springSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Determine the active mode based on priorities and user preferences:
    // Priority: Notification (temporary alert) > Music (if playing or in grace period) > Battery (if charging or <20%) > Idle
    val effectiveMode = when {
        isNotificationEnabled && notificationState.isNotEmpty -> ActiveMode.NOTIFICATION
        isMusicEnabled && (musicState.isPlaying || (musicState.hasTrack && musicState.isDisplayActive)) -> ActiveMode.MUSIC
        isBatteryEnabled && (batteryState.isCharging || batteryState.percentage <= 20) -> ActiveMode.BATTERY
        uiState.activeMode != ActiveMode.IDLE -> uiState.activeMode
        else -> ActiveMode.IDLE
    }

    val isExpanded = uiState.isExpanded

    // Calculate dynamic target width, height, and corner radius
    val targetWidth = if (isExpanded) {
        340.dp
    } else {
        when (effectiveMode) {
            ActiveMode.BATTERY -> (basePillWidthDp + 6).dp
            ActiveMode.MUSIC -> (basePillWidthDp + 48).dp
            ActiveMode.NOTIFICATION -> (basePillWidthDp + 54).dp
            ActiveMode.IDLE -> basePillWidthDp.dp
        }
    }

    val targetHeight = if (isExpanded) {
        when (effectiveMode) {
            ActiveMode.BATTERY -> 146.dp
            ActiveMode.MUSIC -> 180.dp
            ActiveMode.NOTIFICATION -> 136.dp
            ActiveMode.IDLE -> 110.dp
        }
    } else {
        36.dp
    }

    val targetCornerRadius = if (isExpanded) 28.dp else 18.dp

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

    // When collapsed, only wrap the pill (+ bottom headroom for downward drag).
    // When expanded, fill max size to intercept outside clicks on the scrim.
    val rootModifier = if (isExpanded) {
        modifier.fillMaxSize()
    } else {
        modifier.wrapContentSize(Alignment.TopCenter)
    }

    Box(
        modifier = rootModifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // Scrim outside detector (only handles clicks when expanded)
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onCollapseRequest()
                    }
            )
        }

        // The Island Pill / Card
        Box(
            modifier = Modifier
                .padding(top = verticalOffsetDp.dp, bottom = if (isExpanded) 0.dp else 24.dp)
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .width(animatedWidth)
                .height(animatedHeight)
                .shadow(
                    elevation = if (isExpanded) 12.dp else 4.dp,
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .clip(RoundedCornerShape(animatedCornerRadius))
                .background(Color.Black) // Pitch black OLED pill
                // Drag gesture with spring snap-back (active when collapsed)
                .pointerInput(isExpanded) {
                    if (!isExpanded) {
                        detectVerticalDragGestures(
                            onDragEnd = {
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
                                // Apply resistance to drag: clamp between -25 and +50 px
                                val current = dragOffsetY.value
                                val damped = dragAmount * 0.45f
                                val newOffset = (current + damped).coerceIn(-25f, 50f)
                                coroutineScope.launch {
                                    dragOffsetY.snapTo(newOffset)
                                }
                            }
                        )
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (isExpanded) {
                        onCollapseRequest()
                    } else {
                        onExpandRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
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
                                onNextClick = onNextClick
                            )
                        }
                        ActiveMode.NOTIFICATION -> {
                            ExpandedNotificationView(notification = notificationState)
                        }
                        ActiveMode.IDLE -> {
                            // Phase 1: Shell placeholder
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
                            CollapsedNotificationView(notification = notificationState)
                        }
                        ActiveMode.IDLE -> {
                            // Phase 1: Minimal pill shell
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
