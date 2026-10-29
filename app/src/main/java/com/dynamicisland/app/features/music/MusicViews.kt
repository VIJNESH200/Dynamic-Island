package com.dynamicisland.app.features.music

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated 3-bar sound wave equalizer for idle / unknown duration media.
 */
@Composable
fun AudioEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFFD71921)
) {
    val transition = rememberInfiniteTransition(label = "EqualizerTransition")

    val bar1Height by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar1"
    )

    val bar2Height by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isPlaying) 0.2f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar2"
    )

    val bar3Height by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 0.9f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, delayMillis = 50, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(14.dp * bar1Height)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(14.dp * bar2Height)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(14.dp * bar3Height)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
    }
}

/**
 * Collapsed Music Pill (Item 6):
 * - Left: the media app's real icon (e.g. Spotify).
 * - Right: small circular progress ring around play/pause symbol that shows elapsed time.
 *   If duration is unknown, show 3-bar animated equalizer instead.
 * - Width: ~150dp, height: 36dp.
 */
@Composable
fun CollapsedMusicView(
    state: MusicState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIconBitmap = remember(state.packageName) {
        val pkg = state.packageName
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
            .width(150.dp)
            .height(36.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Source app's real icon (e.g. Spotify), or album art, or fallback music note
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap.asImageBitmap(),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (state.albumArt != null) {
            Image(
                bitmap = state.albumArt.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Music",
                tint = Color(0xFFD71921),
                modifier = Modifier.size(18.dp)
            )
        }

        // Right: small circular progress ring around play/pause symbol showing elapsed time
        // If duration is unknown, show 3-bar animated equalizer instead
        if (state.durationMs > 0L) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(22.dp)
            ) {
                CircularProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                    trackColor = Color(0xFF333333)
                )
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        } else {
            AudioEqualizerBars(
                isPlaying = state.isPlaying,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Expanded Music Card (Item 7):
 * - Rounded card, 340dp wide, 28dp corner radius, black background.
 * - Top row: album art (56dp, rounded 12dp), track title (bold), artist (gray).
 * - Middle: thin progress slider with elapsed time on left and total time on right (seeking supported).
 * - Bottom row: Previous, Play/Pause (larger, in a filled circle), Next, using MediaController.
 */
@Composable
fun ExpandedMusicView(
    state: MusicState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        // Top Row: Album Art (56dp, rounded 12dp), Track Title (bold), Artist (gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.albumArt != null) {
                Image(
                    bitmap = state.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = Color(0xFFD71921),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "No Track Playing" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.artist.ifBlank { "Media Session" },
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            AudioEqualizerBars(
                isPlaying = state.isPlaying,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Middle: Progress Slider with Elapsed Time on Left and Total Time on Right
        val maxDuration = if (state.durationMs > 0) state.durationMs.toFloat() else 1f
        val currentPosition = if (isSeeking) seekPositionMs else state.positionMs.toFloat().coerceIn(0f, maxDuration)

        Slider(
            value = currentPosition,
            onValueChange = {
                isSeeking = true
                seekPositionMs = it
            },
            onValueChangeFinished = {
                isSeeking = false
                onSeekTo(seekPositionMs.toLong())
            },
            valueRange = 0f..maxDuration,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF2C2C2E)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isSeeking) state.formatTime(seekPositionMs.toLong()) else state.formattedPosition,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = state.formattedDuration,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Row: Previous, Play/Pause (larger, filled circle), Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play/Pause (larger, in a filled circle)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Next
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
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
fun CollapsedMusicPreview() {
    CollapsedMusicView(
        state = MusicState(
            title = "Gun & Whip",
            artist = "Bassick",
            isPlaying = true,
            durationMs = 195000L,
            positionMs = 45000L,
            packageName = "com.spotify.music"
        )
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ExpandedMusicPreview() {
    ExpandedMusicView(
        state = MusicState(
            title = "Gun & Whip",
            artist = "Bassick",
            isPlaying = true,
            durationMs = 195000L,
            positionMs = 45000L,
            packageName = "com.spotify.music"
        ),
        onPlayPauseClick = {},
        onPreviousClick = {},
        onNextClick = {},
        onSeekTo = {}
    )
}
