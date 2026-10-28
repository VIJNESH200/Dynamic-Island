# Dynamic Island for Android (CMF Phone 2 Pro Edition)

An elegant, animated floating pill-shaped overlay for Android, built with **Kotlin** and **Jetpack Compose**, inspired by the iPhone Dynamic Island and customized specifically for the **CMF Phone 2 Pro** (Nothing OS / Android 14 & 15).

---

## 🌟 Key Features

1. **Floating Pill Overlay (Phase 1: Shell & Physics)**
   - Minimalist pitch-black pill (~126dp x 36dp) centered at the top of the display beneath the status bar.
   - Smooth, bouncy spring-based transitions (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`).
   - Interactive vertical drag gesture with elastic resistance and spring snap-back.
   - Tap pill to expand into a rich ~340dp card; tap outside or tap the pill to smoothly collapse.

2. **Real-time Battery Monitor (Phase 2)**
   - Monitors battery percentage and charging state via `BatteryManager` and `ACTION_BATTERY_CHANGED`.
   - Displays estimated time to full charge (via `computeChargeTimeRemaining()` on Android 9+).
   - Identifies power source: AC Fast Charging, USB, Wireless.
   - Automatic attention banner when charger is connected.

3. **Media Controls & Now Playing (Phase 3)**
   - Connects to active audio sessions via `MediaSessionManager` (Spotify, YouTube Music, Apple Music, etc.).
   - Displays track title, artist name, and animated 3-bar audio equalizer waves.
   - Play/Pause toggle, Previous track, and Next track transport controls.
   - Gracefully hides the music view when playback stops.

4. **Incoming Notifications (Phase 4)**
   - Brief 4.5-second banner in the pill when incoming notifications arrive via `NotificationListenerService`.

5. **Nothing OS & CMF Phone 2 Pro Calibrated**
   - In-app punch-hole calibration slider (0–40dp) to align the pill around the front camera cutout.
   - Foreground service with `specialUse` type and `START_STICKY` for survival against Nothing OS memory killers.
   - In-app guide and shortcut to set battery usage to **Unrestricted**.
   - Pure black OLED styling with Nothing red accents and clean monospace typography.

---

## 📁 Project Architecture

The code is organized into modular packages according to the quality specifications:

```
com.dynamicisland.app/
├── data/
│   └── IslandPreferences.kt          # Persistent settings (toggles, offsets, widths)
├── overlay/
│   ├── DynamicIslandOverlay.kt       # WindowManager coordinator & touch handling
│   ├── OverlayLifecycleOwner.kt      # Custom LifecycleOwner for Compose in WindowManager
│   ├── IslandState.kt                # UI state models and expansion enums
│   └── IslandView.kt                 # Compose root layout with spring & drag physics
├── services/
│   ├── DynamicIslandService.kt       # Foreground service (START_STICKY, Android 14/15 FGS)
│   ├── IslandNotificationListenerService.kt # Notification & MediaSession bridge
│   └── BootReceiver.kt               # Auto-restart on device reboot
├── features/
│   ├── battery/
│   │   ├── BatteryState.kt           # Data class for percentage, time & power source
│   │   ├── BatteryManagerHelper.kt   # System broadcast receiver & time calculations
│   │   └── BatteryViews.kt           # Collapsed pill & expanded card composables
│   ├── music/
│   │   ├── MusicState.kt             # Data class for track, artist, art, playback
│   │   ├── MusicControllerHelper.kt  # MediaController & session callback manager
│   │   └── MusicViews.kt             # Collapsed sound wave & expanded music controls
│   └── notifications/
│       ├── IslandNotification.kt     # Data class for alert banners
│       └── NotificationViews.kt      # Banner composables
└── ui/
    ├── MainActivity.kt               # Permission onboarding & root navigation
    ├── theme/                        # Dark theme only (Nothing OS aesthetic)
    ├── screens/
    │   ├── MainScreen.kt             # Toggle service, test trigger, status cards
    │   ├── SettingsScreen.kt         # Module toggles, CMF camera alignment sliders
    │   └── PermissionDialogs.kt      # Pre-prompt permission explanation modals
    └── components/
        └── NothingComponents.kt      # Styled cards, switches, and status badges
```

---

## 🚀 How to Open in Android Studio

1. Launch **Android Studio** (Ladybug / Koala / Hedgehog or newer).
2. Click **Open** (or **File > Open...**).
3. Select this directory:
   ```
   <PROJECT_FOLDER>
   ```
4. Wait for Gradle to sync. Android Studio will automatically download the required Gradle 8.9 wrapper and dependencies.
5. Ensure your Project SDK is set to **JDK 17** (or JDK 21) under **File > Project Structure > SDK Location / Gradle Settings**.

---

## 🛠️ How to Build

### Using Android Studio
- In the top menu, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
- Once finished, click **locate** to find the output APK at `app/build/outputs/apk/debug/app-debug.apk`.

### Using Command Line (PowerShell / Terminal)
To build the debug APK:
```powershell
.\gradlew assembleDebug
```
To run unit tests:
```powershell
.\gradlew test
```

---

## 📱 How to Install on CMF Phone 2 Pro

### Method 1: Via ADB (Recommended for Developers)
1. On your CMF Phone 2 Pro, enable Developer Options:
   - Go to **Settings > About phone > Device details**.
   - Tap **Build number** 7 times until you see *"You are now a developer!"*.
2. Go to **Settings > System > Developer options** and enable **USB debugging**.
3. Connect your CMF Phone 2 Pro to your PC using a USB-C cable.
4. Run:
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. Or click the green **Run ▶** button in Android Studio with your CMF Phone 2 Pro selected.

### Method 2: Direct File Transfer
1. Copy `app-debug.apk` to your phone via USB or Google Drive.
2. Open the **Files** app on your CMF Phone 2 Pro.
3. Tap `app-debug.apk` and select **Install**. (Allow installation from unknown sources if prompted).

---

## 🔐 How to Grant Permissions

When you launch Dynamic Island for the first time:

1. **"Display over other apps" (Overlay Permission):**
   - Tap **"Grant Permission"** on the main screen.
   - An in-app explanation dialog will detail why it is needed.
   - Tap **"Grant Permission"** to be taken to Nothing OS Settings.
   - Locate **Dynamic Island** in the list and switch the toggle **ON**.
   - Press the Back button to return to the app.

2. **"Notification & Media Access" (For Music & Notifications):**
   - Tap **"Media & Notification Access"** on the main screen.
   - Read the explanation dialog and tap **"Open Settings"**.
   - Locate **Dynamic Island Media & Notifications** and toggle it **ON**.
   - Confirm the system prompt.

3. **Foreground Notification Permission (Android 13+):**
   - Allow the prompt when requested to keep the persistent foreground service active.

---

## 🔋 How to Turn Off Battery Optimization (Nothing OS / CMF Phone 2 Pro)

Nothing OS incorporates aggressive RAM management and battery saving policies ("Deep Sleep") which can terminate background services.

To ensure 24/7 continuous operation:
1. Open the app and tap **"Open Battery Settings"** in the Nothing OS Optimization card (or long-press the Dynamic Island home screen icon > tap `(i)` App info).
2. Tap **App battery usage** (or **Battery**).
3. Change the selection from **"Optimized"** to **"Unrestricted"**.
4. *(Optional Bonus for Nothing OS)*: Open the Nothing OS Recent Apps carousel, tap the **Dynamic Island** icon at the top of the card, and select **Lock**. This prevents the app from being cleared when tapping "Clear All".

---

## ⚠️ Known Limitations on Nothing OS & How They Are Handled

| Limitation / Nothing OS Behavior | Impact | How It Is Handled In This App |
| :--- | :--- | :--- |
| **Punch-hole Camera Alignment** | The CMF Phone 2 Pro has a centered punch-hole camera. A fixed pixel position may misalign if display scaling or font size is changed. | **Dynamic Calibration Slider:** In Settings, a dedicated *Vertical Offset (Y-Axis)* slider allows you to calibrate the pill in 1dp increments from 0dp to 40dp to frame the punch hole pixel-perfectly regardless of display scaling. |
| **Aggressive Process Termination ("Deep Sleep")** | Nothing OS (based on Android 14/15) kills background services to preserve battery life. | **Android 14/15 Compliant FGS:** Service is declared with `android:foregroundServiceType="specialUse"` and configured with `START_STICKY`. The app automatically restarts via `onStartCommand` and handles `BOOT_COMPLETED` through `BootReceiver`. |
| **Touch Events Outside Expanded Card** | Commercial overlay apps often block touches across the screen when expanded or fail to dismiss when tapping outside. | **Dual-State Window Bounds:** In collapsed state, the WindowManager layout is `WRAP_CONTENT` (underlying apps receive all touches). When expanded to 340dp, the layout switches to `MATCH_PARENT` with a transparent touch-scrim that captures taps outside the card, plays the bouncy collapse spring, and resets the bounds. |
| **Media Metadata Delay on Nothing OS** | When skipping tracks on Spotify or YouTube Music, metadata callbacks can arrive asynchronously. | **Reactive StateFlow & Fallback:** `MusicControllerHelper` registers both active session listeners and per-controller callbacks (`onPlaybackStateChanged`, `onMetadataChanged`). When playback ceases, it gracefully pauses and collapses the pill. |
| **ComposeView Inside WindowManager Crash** | Attaching Jetpack Compose outside an Activity normally crashes with `ViewTreeLifecycleOwner not found`. | **OverlayLifecycleOwner:** Implements custom `SavedStateRegistryOwner`, `ViewModelStoreOwner`, and `LifecycleOwner` attached directly to the `ComposeView`, ensuring zero lifecycle crashes. |
