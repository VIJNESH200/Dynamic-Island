package com.dynamicisland.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dynamicisland.app.data.IslandPreferences
import com.dynamicisland.app.services.DynamicIslandService
import com.dynamicisland.app.services.IslandNotificationListenerService
import com.dynamicisland.app.ui.screens.MainScreen
import com.dynamicisland.app.ui.screens.PermissionExplanationDialog
import com.dynamicisland.app.ui.screens.SettingsScreen
import com.dynamicisland.app.ui.theme.DynamicIslandTheme

enum class Screen {
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var preferences: IslandPreferences

    private var currentScreen by mutableStateOf(Screen.MAIN)
    private var isOverlayGranted by mutableStateOf(false)
    private var isNotificationAccessGranted by mutableStateOf(false)

    // Dialog trigger states
    private var showOverlayExplanation by mutableStateOf(false)
    private var showNotificationAccessExplanation by mutableStateOf(false)
    private var showBatteryOptimizationExplanation by mutableStateOf(false)

    // Permission launcher for Android 13+ POST_NOTIFICATIONS
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Post notifications result
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = IslandPreferences.getInstance(this)

        // Request POST_NOTIFICATIONS on Android 13+ for foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            DynamicIslandTheme {
                val isServiceRunning by DynamicIslandService.isServiceRunning.collectAsState()

                when (currentScreen) {
                    Screen.MAIN -> {
                        MainScreen(
                            isServiceRunning = isServiceRunning,
                            isOverlayPermissionGranted = isOverlayGranted,
                            isNotificationAccessGranted = isNotificationAccessGranted,
                            onToggleService = { enable ->
                                if (enable) {
                                    if (Settings.canDrawOverlays(this)) {
                                        DynamicIslandService.start(this)
                                        preferences.setIslandEnabled(true)
                                    } else {
                                        showOverlayExplanation = true
                                    }
                                } else {
                                    DynamicIslandService.stop(this)
                                    preferences.setIslandEnabled(false)
                                }
                            },
                            onRequestOverlayPermission = {
                                showOverlayExplanation = true
                            },
                            onRequestNotificationAccess = {
                                showNotificationAccessExplanation = true
                            },
                            onRequestBatteryOptimization = {
                                showBatteryOptimizationExplanation = true
                            },
                            onOpenSettings = {
                                currentScreen = Screen.SETTINGS
                            }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            preferences = preferences,
                            onNavigateBack = {
                                currentScreen = Screen.MAIN
                            }
                        )
                    }
                }

                // Overlay Permission Explanation Dialog
                if (showOverlayExplanation) {
                    PermissionExplanationDialog(
                        title = "Display Over Other Apps",
                        description = "Dynamic Island must be drawn over other applications to float at the top center of your screen below the status bar.\n\nTap below to open system settings and enable the toggle for Dynamic Island.",
                        icon = Icons.Default.Layers,
                        confirmButtonText = "Grant Permission",
                        onConfirm = {
                            showOverlayExplanation = false
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        },
                        onDismiss = { showOverlayExplanation = false }
                    )
                }

                // Notification Access Explanation Dialog
                if (showNotificationAccessExplanation) {
                    PermissionExplanationDialog(
                        title = "Notification & Media Access",
                        description = "To control music playback (Spotify, YouTube Music) and display track titles and incoming notification badges, Android requires Notification Access to link with MediaSessionManager.\n\nTap below to find Dynamic Island in the list and allow access.",
                        icon = Icons.Default.MusicNote,
                        confirmButtonText = "Open Settings",
                        onConfirm = {
                            showNotificationAccessExplanation = false
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            startActivity(intent)
                        },
                        onDismiss = { showNotificationAccessExplanation = false }
                    )
                }

                // Nothing OS Battery Optimization Dialog
                if (showBatteryOptimizationExplanation) {
                    PermissionExplanationDialog(
                        title = "Nothing OS Battery Settings",
                        description = "Nothing OS will automatically put background apps to sleep.\n\nOn the next screen:\n1. Tap 'Battery' (or 'App battery usage')\n2. Select 'Unrestricted'\n\nThis keeps your Dynamic Island floating seamlessly without interruption.",
                        icon = Icons.Default.BatteryChargingFull,
                        confirmButtonText = "Open App Info",
                        onConfirm = {
                            showBatteryOptimizationExplanation = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        },
                        onDismiss = { showBatteryOptimizationExplanation = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()

        // Auto-start service if permissions are granted and user preference is enabled
        if (isOverlayGranted && preferences.isIslandEnabled.value && !DynamicIslandService.isServiceRunning.value) {
            DynamicIslandService.start(this)
        }
    }

    private fun refreshPermissionStates() {
        isOverlayGranted = Settings.canDrawOverlays(this)

        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        isNotificationAccessGranted = enabledListeners.contains(packageName)
    }
}
