package com.dynamicisland.app.features.notifications

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact pill notification banner:
 * - Position: directly below status bar (same top offset as collapsed pill + 4dp gap)
 * - Max width: 340dp, centered
 * - Shape: 28dp corner radius, black background, subtle 1dp border (white at 8% opacity)
 * - Left: source app's real icon from PackageManager (never a red bell icon)
 * - Right: app name in small gray text on top, title in bold white, body in small gray text below.
 *   Truncated to one line each.
 */
@Composable
fun CompactNotificationBannerView(
    notification: IslandNotification,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIconBitmap = remember(notification.packageName) {
        val pkg = notification.packageName
        if (!pkg.isNullOrBlank()) {
            try {
                val drawable = context.packageManager.getApplicationIcon(pkg)
                drawableToBitmap(drawable)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Row(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source app's real icon from PackageManager (no red bell!)
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap.asImageBitmap(),
                contentDescription = notification.appName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right side: 3 lines truncated to 1 line each
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // App name in small gray text on top
            Text(
                text = notification.appName.ifBlank { "Notification" },
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Title in bold white
            Text(
                text = notification.title.ifBlank { "Notification" },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Body in small gray text below
            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    color = Color(0xFFB0B0B5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CollapsedNotificationView(
    notification: IslandNotification,
    modifier: Modifier = Modifier
) {
    CompactNotificationBannerView(notification = notification, modifier = modifier)
}

@Composable
fun ExpandedNotificationView(
    notification: IslandNotification,
    modifier: Modifier = Modifier
) {
    CompactNotificationBannerView(notification = notification, modifier = modifier)
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun CompactNotificationPreview() {
    CompactNotificationBannerView(
        notification = IslandNotification(
            title = "Gun & Whip",
            text = "Bassick",
            appName = "Spotify",
            packageName = "com.spotify.music"
        )
    )
}
