# Dynamic Island for Android (CMF Phone 2 Pro Edition)

An elegant, animated floating pill-shaped overlay for Android, built with **Kotlin** and **Jetpack Compose**, inspired by the iPhone Dynamic Island and customized specifically for the **CMF Phone 2 Pro** (Nothing OS / Android 14 & 15).

---

## 🌟 Key Features

1. **Floating Pill Overlay (Shell, Gestures & Physics)**
   - Minimalist pitch-black pill centered at the top of the display beneath the status bar.
   - Smooth, bouncy spring-based transitions (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`).
   - **Intuitive Swipe Gestures**:
     - **Swipe UP**: Dismiss the island immediately; stays dismissed until the next active trigger (charger, music track change, incoming notification, or timer).
     - **Swipe DOWN**: On the collapsed pill, expands it smoothly into the full card.
     - **Tap**: Expands collapsed pill, or collapses expanded card.
   - **Auto-Hide Inactivity Timer**: Island automatically hides after 5 seconds by default (configurable to 3s, 5s, 10s, or Never in Settings). Any touch, drag, or interaction restarts the countdown from zero.
   - **Zero Touch Blocking**: Window bounds are tightly sized to the pill / card (`WRAP_CONTENT` x `WRAP_CONTENT`). Uses `FLAG_WATCH_OUTSIDE_TOUCH`, `FLAG_NOT_TOUCH_MODAL`, and `FLAG_NOT_FOCUSABLE` so taps outside the card pass directly to underlying apps and status bar icons.

2. **On-Demand Event Triggers (No Always-On Pill)**
   - The pill is hidden by default and removed from the screen when idle to avoid visual clutter.
   - Appears dynamically only when active triggers occur:
     - **Charger Plug / Unplug**: Displays battery percentage and charging state for 4 seconds, then auto-hides.
     - **Music Playback**: Appears in collapsed state when media plays; auto-hides 6 seconds after pausing. Does not auto-expand.
     - **Incoming Notifications**: Displays a compact 340dp pill banner with real app icon, app name, title, and body for 5 seconds, then auto-hides.
     - **Active Timer & Stopwatch**: Stays visible while a countdown timer or stopwatch runs.
   - Each trigger can be independently toggled in Settings.

3. **Music in Collapsed Pill & Expanded Card**
   - **Collapsed Pill (150dp x 36dp)**: Displays the actual source app icon (e.g. Spotify, YouTube Music) on the left via `PackageManager.getApplicationIcon()`. On the right, displays a circular progress ring around a play/pause symbol showing elapsed playback time (or a 3-bar animated equalizer if duration is unknown).
   - **Expanded Card (340dp, 28dp radius)**: Displays 56dp album artwork (rounded 12dp), track title (bold), artist name (gray), a seekable thin progress slider updated every 500ms with elapsed/total timestamps, and Previous, Play/Pause (in a filled circle), and Next controls via `MediaController.transportControls`. Tested for seamless background playback with Spotify.

4. **Status Bar Clearance & Alignment Calibration**
   - Sits cleanly below the status bar with automatic runtime status bar height detection and a 4dp gap, plus an in-app fine-tuning slider (0–40dp).
   - Status bar icons (clock, signal, battery) remain fully visible and tappable in both collapsed and expanded states.
   - First-time launch presentation: plays a ~1.5s expansion animation, then collapses into position below the status bar. (Third-party apps cannot hide the system status bar).

5. **Nothing OS & CMF Phone 2 Pro Calibrated**
   - Foreground service with `specialUse` type and `START_STICKY` for survival against Nothing OS memory killers.
   - Foreground service notification configured with `IMPORTANCE_MIN` and text `"Dynamic Island is active"` for minimal intrusion.
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
| **Status Bar & Icon Clearance** | The CMF Phone 2 Pro has a centered camera cutout and status bar elements (clock, notifications, battery). Overlays can obscure these icons if placed too high. | **Automatic Runtime Status Bar Height Detection:** At runtime, the overlay queries the system status bar height via `WindowInsets` / `status_bar_height` and places the pill 4dp below the status bar so the clock, signal, and battery indicators remain completely unobstructed. An extra *Vertical Offset (Y-Axis)* slider (0dp–40dp) in Settings allows user fine-tuning. |
| **Aggressive Process Termination ("Deep Sleep")** | Nothing OS (based on Android 14/15) kills background services to preserve battery life. | **Android 14/15 Compliant FGS:** Service is declared with `android:foregroundServiceType="specialUse"` and configured with `START_STICKY`. The app automatically restarts via `onStartCommand` and handles `BOOT_COMPLETED` through `BootReceiver`. |
| **Touch Events & Status Bar Access** | Commercial overlay apps often block touches across the screen when expanded or block status bar icons. | **Sized Window Bounds & Outside Touch Watching:** Both collapsed and expanded states use `WRAP_CONTENT` bounds positioned strictly below the status bar (`y = topOffsetPx`). WindowManager flags include `FLAG_WATCH_OUTSIDE_TOUCH`, `FLAG_NOT_FOCUSABLE`, and `FLAG_NOT_TOUCH_MODAL`. Taps outside the pill or card fall through directly to underlying apps and status bar icons, while outside touches smoothly collapse the expanded card without intercepting the user's touch. |
| **Media Metadata Delay on Nothing OS** | When skipping tracks on Spotify or YouTube Music, metadata callbacks can arrive asynchronously. | **Reactive StateFlow & Fallback:** `MusicControllerHelper` registers both active session listeners and per-controller callbacks (`onPlaybackStateChanged`, `onMetadataChanged`). When playback ceases, it gracefully pauses and collapses the pill. |
| **ComposeView Inside WindowManager Crash** | Attaching Jetpack Compose outside an Activity normally crashes with `ViewTreeLifecycleOwner not found`. | **OverlayLifecycleOwner:** Implements custom `SavedStateRegistryOwner`, `ViewModelStoreOwner`, and `LifecycleOwner` attached directly to the `ComposeView`, ensuring zero lifecycle crashes. |
