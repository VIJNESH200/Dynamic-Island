# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in <ANDROID_SDK_PATH>/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep Compose internal rules
-keep class androidx.compose.** { *; }

# Keep data classes and models
-keep class com.dynamicisland.app.features.** { *; }
-keep class com.dynamicisland.app.overlay.** { *; }
