package com.dynamicisland.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.dynamicisland.app.data.IslandPreferences

/**
 * BroadcastReceiver triggered when the device finishes booting or the app is updated.
 * Automatically restarts DynamicIslandService if the user enabled it.
 */
class BootReceiver : BroadcastReceiver() {

    private val tag = "DynamicIslandBoot"

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        Log.d(tag, "Received broadcast action: ${intent.action}")

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )

        if (intent.action in validActions) {
            val prefs = IslandPreferences.getInstance(context)
            if (prefs.isIslandEnabled.value && Settings.canDrawOverlays(context)) {
                Log.d(tag, "Starting DynamicIslandService on boot")
                DynamicIslandService.start(context)
            } else {
                Log.d(tag, "Dynamic Island is either disabled or missing overlay permission; not starting on boot")
            }
        }
    }
}
