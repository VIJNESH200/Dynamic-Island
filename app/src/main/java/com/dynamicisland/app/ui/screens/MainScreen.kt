package com.dynamicisland.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.dynamicisland.app.features.updater.AppUpdateCard
import com.dynamicisland.app.features.updater.UpdateManager
import com.dynamicisland.app.features.notifications.IslandNotification
import com.dynamicisland.app.services.DynamicIslandService
import com.dynamicisland.app.ui.components.NothingCard
import com.dynamicisland.app.ui.components.NothingSwitch
import com.dynamicisland.app.ui.components.StatusBadge
import com.dynamicisland.app.ui.theme.BatteryGreen
import com.dynamicisland.app.ui.theme.DarkSurface
import com.dynamicisland.app.ui.theme.NothingDarkGray
import com.dynamicisland.app.ui.theme.NothingGrayText
import com.dynamicisland.app.ui.theme.NothingRed
import com.dynamicisland.app.ui.theme.NothingWhite
import com.dynamicisland.app.ui.theme.PureBlack

@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    isOverlayPermissionGranted: Boolean,
    isNotificationAccessGranted: Boolean,
    onToggleService: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }

    LaunchedEffect(Unit) {
        // Silently check if a newer release exists on GitHub
        updateManager.checkForUpdates()
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC ISLAND",
                        color = NothingWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CMF Phone 2 Pro Edition",
                        color = NothingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NothingWhite
                    )
                }
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
            // Master Switch Card
            NothingCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) NothingRed.copy(alpha = 0.15f) else NothingDarkGray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isServiceRunning) NothingRed else NothingGrayText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Dynamic Island",
                                    color = NothingWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isServiceRunning) "Running in foreground" else "Service is paused",
                                    color = NothingGrayText,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        NothingSwitch(
                            checked = isServiceRunning,
                            onCheckedChange = { checked ->
                                if (checked && !isOverlayPermissionGranted) {
                                    onRequestOverlayPermission()
                                } else {
                                    onToggleService(checked)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            isActive = isServiceRunning,
                            activeText = "OVERLAY ACTIVE",
                            inactiveText = "STOPPED"
                        )

                        Text(
                            text = if (isServiceRunning) "Tap pill to expand" else "Turn on switch to start",
                            color = NothingGrayText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Warning Banner if Overlay Permission Missing
            AnimatedVisibility(visible = !isOverlayPermissionGranted) {
                NothingCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = NothingRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overlay Permission Required",
                                color = NothingWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Allow 'Display over other apps' so the pill can float at the top.",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestOverlayPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Grant",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Permissions Status Section
            Text(
                text = "PERMISSIONS & ACCESS",
                color = NothingGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            // Overlay Permission Card
            NothingCard(
                modifier = Modifier.clickable { onRequestOverlayPermission() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isOverlayPermissionGranted) BatteryGreen else NothingRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Display Over Other Apps",
                                color = NothingWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isOverlayPermissionGranted) "Granted • Overlay ready" else "Required to draw floating pill",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    StatusBadge(
                        isActive = isOverlayPermissionGranted,
                        activeText = "GRANTED",
                        inactiveText = "REQUIRED"
                    )
                }
            }

            // Notification Access Card (For Music & Notifications)
            NothingCard(
                modifier = Modifier.clickable { onRequestNotificationAccess() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isNotificationAccessGranted) BatteryGreen else NothingRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Media & Notification Access",
                                color = NothingWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isNotificationAccessGranted) "Granted • Spotify/YT Music synced" else "Needed for media metadata & controls",
                                color = NothingGrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    StatusBadge(
                        isActive = isNotificationAccessGranted,
                        activeText = "GRANTED",
                        inactiveText = "REQUIRED"
                    )
                }
            }

            // Nothing OS & CMF Phone 2 Pro Battery Optimization Tip Card
            Text(
                text = "NOTHING OS OPTIMIZATION",
                color = NothingGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            NothingCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NothingRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = NothingRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Prevent Nothing OS Sleep",
                            color = NothingWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Nothing OS and Android 14/15 aggressively terminate background services unless battery optimization is disabled.",
                        color = NothingGrayText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To guarantee 24/7 reliability on CMF Phone 2 Pro:\n" +
                                "1. Tap button below to open App Info\n" +
                                "2. Tap 'Battery' or 'App battery usage'\n" +
                                "3. Change from 'Optimized' to 'Unrestricted'",
                        color = Color(0xFFE5E5EA),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onRequestBatteryOptimization,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Open Battery Settings",
                            color = NothingWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick Test Button
            NothingCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "QUICK TEST",
                        color = NothingGrayText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Test temporary notification alert banner on the floating pill:",
                        color = NothingGrayText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            DynamicIslandService.postNotification(
                                IslandNotification(
                                    title = "Test notification",
                                    text = "This is a sample alert",
                                    appName = "Sample App",
                                    packageName = ""
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = NothingRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trigger Sample Notification",
                            color = NothingWhite,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DynamicIslandService.triggerChargerAlert()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = BatteryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trigger Charger Alert (4s)",
                            color = NothingWhite,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // In-app updater card
            AppUpdateCard(updateManager = updateManager)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MainScreenPreview() {
    MainScreen(
        isServiceRunning = true,
        isOverlayPermissionGranted = true,
        isNotificationAccessGranted = true,
        onToggleService = {},
        onRequestOverlayPermission = {},
        onRequestNotificationAccess = {},
        onRequestBatteryOptimization = {},
        onOpenSettings = {}
    )
}
