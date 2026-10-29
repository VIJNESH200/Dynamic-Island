package com.dynamicisland.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.ui.components.NothingCard
import com.dynamicisland.app.ui.components.NothingSwitch
import com.dynamicisland.app.ui.theme.BatteryGreen
import com.dynamicisland.app.ui.theme.DarkSurface
import com.dynamicisland.app.ui.theme.NothingDarkGray
import com.dynamicisland.app.ui.theme.NothingGrayText
import com.dynamicisland.app.ui.theme.NothingRed
import com.dynamicisland.app.ui.theme.NothingWhite
import com.dynamicisland.app.ui.theme.PureBlack

@Composable
fun SettingsScreen(
    preferences: IslandPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val isBatteryEnabled by preferences.isBatteryFeatureEnabled.collectAsState()
    val isMusicEnabled by preferences.isMusicFeatureEnabled.collectAsState()
    val isNotificationEnabled by preferences.isNotificationFeatureEnabled.collectAsState()
    val isTimerEnabled by preferences.isTimerFeatureEnabled.collectAsState()
    val autoHideSeconds by preferences.autoHideSeconds.collectAsState()
    val verticalOffset by preferences.verticalOffsetDp.collectAsState()
    val pillWidth by preferences.pillWidthDp.collectAsState()

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NothingWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    color = NothingWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trigger Settings Section
            Column {
                Text(
                    text = "TRIGGER SETTINGS",
                    color = NothingGrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The pill is hidden by default and appears only when active events trigger it.",
                    color = NothingGrayText,
                    fontSize = 12.sp
                )
            }

            // Charger Plug / Unplug Trigger Toggle
            NothingCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (isBatteryEnabled) BatteryGreen else NothingGrayText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Charger Plug / Unplug",
                                color = NothingWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Show battery status for 4s, then hide",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NothingSwitch(
                        checked = isBatteryEnabled,
                        onCheckedChange = { preferences.setBatteryFeatureEnabled(it) }
                    )
                }
            }

            // Music Playback Trigger Toggle
            NothingCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isMusicEnabled) NothingRed else NothingGrayText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Music Playback",
                                color = NothingWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Visible while playing; hides 6s after pause",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NothingSwitch(
                        checked = isMusicEnabled,
                        onCheckedChange = { preferences.setMusicFeatureEnabled(it) }
                    )
                }
            }

            // Notification Trigger Toggle
            NothingCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isNotificationEnabled) NothingRed else NothingGrayText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Incoming Notifications",
                                color = NothingWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Briefly pop up banner, then auto-hide",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NothingSwitch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { preferences.setNotificationFeatureEnabled(it) }
                    )
                }
            }

            // Timer Trigger Toggle
            NothingCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (isTimerEnabled) Color(0xFFFF9500) else NothingGrayText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Active Timer & Countdown",
                                color = NothingWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Visible while clock timer or stopwatch runs",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NothingSwitch(
                        checked = isTimerEnabled,
                        onCheckedChange = { preferences.setTimerFeatureEnabled(it) }
                    )
                }
            }

            // Auto-Hide Timeout Section
            Text(
                text = "AUTO-HIDE TIMEOUT",
                color = NothingGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            NothingCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Idle Disappear Delay",
                        color = NothingWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Automatically hide the island after inactivity. Any touch on the island restarts the timer.",
                        color = NothingGrayText,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            3 to "3s",
                            5 to "5s",
                            10 to "10s",
                            -1 to "Never"
                        )
                        options.forEach { (seconds, label) ->
                            val isSelected = autoHideSeconds == seconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NothingRed else DarkSurface)
                                    .clickable { preferences.setAutoHideSeconds(seconds) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) NothingWhite else Color(0xFFCCCCCC),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Vertical Alignment Calibration
            Text(
                text = "VERTICAL ALIGNMENT",
                color = NothingGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            NothingCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vertical Offset (Y-Axis)",
                            color = NothingWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "+${verticalOffset} dp",
                            color = NothingRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extra spacing below status bar (fine-tune placement).",
                        color = NothingGrayText,
                        fontSize = 12.sp
                    )

                    Slider(
                        value = verticalOffset.toFloat(),
                        onValueChange = { preferences.setVerticalOffsetDp(it.toInt()) },
                        valueRange = 0f..40f,
                        steps = 40,
                        colors = SliderDefaults.colors(
                            thumbColor = NothingRed,
                            activeTrackColor = NothingRed,
                            inactiveTrackColor = NothingDarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Collapsed Pill Width",
                            color = NothingWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${pillWidth} dp",
                            color = NothingRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Width of the pill when in collapsed idle state.",
                        color = NothingGrayText,
                        fontSize = 12.sp
                    )

                    Slider(
                        value = pillWidth.toFloat(),
                        onValueChange = { preferences.setPillWidthDp(it.toInt()) },
                        valueRange = 100f..180f,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = NothingRed,
                            activeTrackColor = NothingRed,
                            inactiveTrackColor = NothingDarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            preferences.setVerticalOffsetDp(IslandPreferences.DEFAULT_OFFSET_DP)
                            preferences.setPillWidthDp(IslandPreferences.DEFAULT_PILL_WIDTH_DP)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = NothingWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset Alignment to Defaults",
                            color = NothingWhite,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
