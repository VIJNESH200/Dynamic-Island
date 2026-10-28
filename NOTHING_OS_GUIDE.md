# CMF Phone 2 Pro & Nothing OS Compatibility Guide

This guide details the technical considerations, display characteristics, and OS behaviors specific to the **CMF Phone 2 Pro** running **Nothing OS** (Android 14 / Android 15).

---

## 1. Display & Punch-Hole Camera Cutout

- **Camera Cutout:** Centered circular punch-hole at the top edge.
- **Status Bar & Scaling:** Status bar height and display metrics vary based on user display zoom and font scaling in system settings.

### Alignment Strategy:
In `IslandPreferences.kt`, the default vertical offset is set to `12dp` (from the top of the display surface), placing the top edge of the collapsed pill right above the camera punch-hole so that the pill neatly encloses and frames the camera hole, giving the authentic iPhone Dynamic Island feel. 

Because exact resolutions and display scaling can differ across CMF / Nothing device variants and OS updates (check your device under **Settings > About phone**), you do not need to rely on hardcoded numbers: use the in-app **Settings > Vertical Offset (Y-Axis)** slider (`0dp` to `40dp`) to calibrate the pill pixel-perfectly on your screen in real time.

---

## 2. Nothing OS Background Service Management

Nothing OS features proprietary battery management algorithms ("Smart Battery" / "Intelligent Power Saving") designed to maximize screen-on time by terminating long-running background tasks.

### Mitigations Implemented in Code:

1. **Foreground Service with Persistent Notification Channel:**
   ```kotlin
   NotificationChannel(
       CHANNEL_ID,
       "Dynamic Island Overlay",
       NotificationManager.IMPORTANCE_LOW
   )
   ```
   Uses `IMPORTANCE_LOW` so there is no disruptive chime or vibration, but the system process priority is elevated to `FOREGROUND_SERVICE`.

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
