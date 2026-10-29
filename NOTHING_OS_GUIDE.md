# CMF Phone 2 Pro & Nothing OS Compatibility Guide

This guide details the technical considerations, display characteristics, and OS behaviors specific to the **CMF Phone 2 Pro** running **Nothing OS** (Android 14 / Android 15).

---

## 1. Display & Status Bar Clearance

- **Camera Cutout:** Centered circular punch-hole at the top edge.
- **Status Bar & Insets:** The status bar houses the clock, notification icons, and battery indicators. Status bar height varies dynamically based on user display zoom and font scaling in system settings.

### Alignment Strategy:
Rather than overlapping the status bar or covering the clock, signal, and battery icons, the pill is positioned cleanly **below** the status bar. At runtime, the overlay queries the real system status bar height via `WindowInsets.Type.statusBars()` (Android 11+) or the system resource `status_bar_height`, and positions the pill with a 4dp gap below it. This ensures the clock, signal bars, battery icons, and camera cutout remain completely visible and unobstructed.

To accommodate different personal preferences or custom themes, the in-app **Settings > Vertical Offset (Y-Axis)** slider (`0dp` to `40dp`) adds extra vertical spacing on top of the calculated status bar clearance.

---

## 2. Nothing OS Background Service Management

Nothing OS features proprietary battery management algorithms ("Smart Battery" / "Intelligent Power Saving") designed to maximize screen-on time by terminating long-running background tasks.

### Mitigations Implemented in Code:

1. **Foreground Service with Persistent Notification Channel:**
   ```kotlin
   NotificationChannel(
       CHANNEL_ID,
       "Dynamic Island",
       NotificationManager.IMPORTANCE_MIN
   )
   ```
   Uses `IMPORTANCE_MIN` so there is zero intrusive sound, vibration, or status bar clutter, while maintaining foreground service execution priority.

2. **Android 14/15 Foreground Service Type:**
   Starting in Android 14 (API 34), generic foreground services are restricted. In `AndroidManifest.xml`:
   ```xml
   <service
       android:name=".services.DynamicIslandService"
       android:foregroundServiceType="specialUse">
       <property
           android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
           android:value="dynamic_island_overlay" />
   </service>
   ```

3. **`START_STICKY` Return Flag:**
   If Nothing OS kills the service during severe RAM pressure (e.g., launching heavy 3D games), the system recreates the service as soon as RAM recovers:
   ```kotlin
   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       return START_STICKY
   }
   ```

4. **Boot & App Update Auto-Start:**
   `BootReceiver` listens to `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` to relaunch the island automatically without requiring the user to open the app after every reboot.

5. **In-App Direct Intent to Battery Settings:**
   The app provides a direct one-tap button that takes the user to Nothing OS's App Info page to switch battery optimization to **Unrestricted**.

---

## 3. MediaSessionManager on Nothing OS

Android limits access to active media sessions (used by Spotify, YouTube Music, SoundCloud, etc.) to services that hold the `BIND_NOTIFICATION_LISTENER_SERVICE` permission.

- `IslandNotificationListenerService` acts as the security context provider for `MediaSessionManager.getActiveSessions()`.
- When the user plays media, callbacks trigger real-time updates.
- If the media app closes, `MusicControllerHelper` handles `onSessionDestroyed()` and gracefully reverts the island to the idle/battery pill state.
