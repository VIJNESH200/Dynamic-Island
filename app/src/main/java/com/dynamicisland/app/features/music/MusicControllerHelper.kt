package com.dynamicisland.app.features.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.dynamicisland.app.services.IslandNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages interaction with Android's MediaSessionManager and active MediaControllers.
 * Requires notification access (BIND_NOTIFICATION_LISTENER_SERVICE).
 */
class MusicControllerHelper(private val context: Context) {

    private val tag = "MusicControllerHelper"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val helperScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _musicState = MutableStateFlow(MusicState())
    val musicState: StateFlow<MusicState> = _musicState.asStateFlow()

    private var activeController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private val registeredControllers = mutableMapOf<MediaController, MediaController.Callback>()

    private var playbackTickerJob: Job? = null
    private var pauseGracePeriodJob: Job? = null

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        mainHandler.post {
            handleActiveSessionsChanged(controllers)
        }
    }

    fun start() {
        try {
            IslandNotificationListenerService.requestRebindIfDisconnected(context)
            Log.d("DI_MUSIC", "Starting MusicControllerHelper. Listener connected: ${IslandNotificationListenerService.isConnected}")

            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val notificationListener = ComponentName(context, IslandNotificationListenerService::class.java)

            // Register active sessions listener
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                notificationListener
            )
            Log.d("DI_MUSIC", "Registered addOnActiveSessionsChangedListener")

            // Initial query
            findAndAttachActiveSession()
        } catch (e: SecurityException) {
            Log.w("DI_MUSIC", "Notification access not granted for MediaSessionManager: ${e.message}")
            _musicState.value = MusicState()
        } catch (e: Exception) {
            Log.e("DI_MUSIC", "Error starting MusicControllerHelper: ${e.message}")
            _musicState.value = MusicState()
        }
    }

    fun stop() {
        try {
            playbackTickerJob?.cancel()
            pauseGracePeriodJob?.cancel()
            cleanupControllers()
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            Log.d("DI_MUSIC", "Stopped MusicControllerHelper")
        } catch (e: Exception) {
            Log.w(tag, "Error stopping MusicControllerHelper: ${e.message}")
        }
    }

    private fun cleanupControllers() {
        for ((c, cb) in registeredControllers) {
            try {
                c.unregisterCallback(cb)
            } catch (_: Exception) {}
        }
        registeredControllers.clear()
        activeController = null
    }

    fun play() {
        try {
            activeController?.transportControls?.play()
        } catch (e: Exception) {
            Log.e(tag, "Failed to invoke play: ${e.message}")
        }
    }

    fun pause() {
        try {
            activeController?.transportControls?.pause()
        } catch (e: Exception) {
            Log.e(tag, "Failed to invoke pause: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_musicState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun skipToNext() {
        try {
            activeController?.transportControls?.skipToNext()
        } catch (e: Exception) {
            Log.e(tag, "Failed to skipToNext: ${e.message}")
        }
    }

    fun skipToPrevious() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            Log.e(tag, "Failed to skipToPrevious: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            activeController?.transportControls?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e(tag, "Failed to seekTo: ${e.message}")
        }
    }

    private fun findAndAttachActiveSession() {
        IslandNotificationListenerService.requestRebindIfDisconnected(context)
        val manager = mediaSessionManager ?: return
        val component = ComponentName(context, IslandNotificationListenerService::class.java)
        try {
            val controllers = manager.getActiveSessions(component)
            Log.d("DI_MUSIC", "getActiveSessions returned ${controllers?.size ?: 0} sessions")
            handleActiveSessionsChanged(controllers)
        } catch (e: SecurityException) {
            Log.w("DI_MUSIC", "Cannot access active sessions without notification listener permission: ${e.message}")
        } catch (e: Exception) {
            Log.e("DI_MUSIC", "Error querying active sessions: ${e.message}")
        }
    }

    private fun handleActiveSessionsChanged(controllers: List<MediaController>?) {
        Log.d("DI_MUSIC", "handleActiveSessionsChanged count: ${controllers?.size ?: 0}")
        if (controllers.isNullOrEmpty()) {
            playbackTickerJob?.cancel()
            pauseGracePeriodJob?.cancel()
            cleanupControllers()
            _musicState.value = MusicState()
            return
        }

        val currentControllers = controllers

        // Unregister controllers no longer present
        val toRemove = registeredControllers.keys.filter { existing ->
            currentControllers.none { it.sessionToken == existing.sessionToken }
        }
        for (c in toRemove) {
            val cb = registeredControllers.remove(c)
            if (cb != null) {
                try {
                    c.unregisterCallback(cb)
                    Log.d("DI_MUSIC", "Unregistered callback for removed controller: ${c.packageName}")
                } catch (e: Exception) {
                    Log.w("DI_MUSIC", "Error unregistering callback: ${e.message}")
                }
            }
        }

        // Register a callback on each active MediaController
        for (controller in currentControllers) {
            val alreadyRegistered = registeredControllers.keys.any { it.sessionToken == controller.sessionToken }
            if (!alreadyRegistered) {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        Log.d("DI_MUSIC", "onPlaybackStateChanged: pkg=${controller.packageName}, state=${state?.state}")
                        if (state?.state == PlaybackState.STATE_PLAYING) {
                            activeController = controller
                        }
                        updateFromController(activeController ?: controller)
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        Log.d("DI_MUSIC", "onMetadataChanged: pkg=${controller.packageName}")
                        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                            activeController = controller
                        }
                        updateFromController(activeController ?: controller)
                    }

                    override fun onSessionDestroyed() {
                        Log.d("DI_MUSIC", "onSessionDestroyed: pkg=${controller.packageName}")
                        registeredControllers.remove(controller)
                        if (activeController?.sessionToken == controller.sessionToken) {
                            activeController = null
                            playbackTickerJob?.cancel()
                            pauseGracePeriodJob?.cancel()
                            _musicState.value = MusicState()
                            findAndAttachActiveSession()
                        }
                    }
                }
                try {
                    controller.registerCallback(callback, mainHandler)
                    registeredControllers[controller] = callback
                    Log.d("DI_MUSIC", "Registered callback on controller: ${controller.packageName}")
                } catch (e: Exception) {
                    Log.e("DI_MUSIC", "Failed to register callback for ${controller.packageName}: ${e.message}")
                }
            }
        }

        // Prioritize a controller currently in STATE_PLAYING
        val playingController = currentControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: activeController ?: currentControllers.firstOrNull()

        activeController = playingController
        updateFromController(activeController)
    }

    private fun updateFromController(controller: MediaController?) {
        if (controller == null) {
            playbackTickerJob?.cancel()
            pauseGracePeriodJob?.cancel()
            _musicState.value = MusicState()
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.description?.title?.toString()
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.description?.subtitle?.toString()
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.description?.iconBitmap

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawPosition = playbackState?.position ?: 0L
        val lastUpdateTime = playbackState?.lastPositionUpdateTime ?: 0L
        val speed = playbackState?.playbackSpeed ?: 1.0f

        val currentPosition = if (isPlaying && lastUpdateTime > 0L) {
            val elapsed = SystemClock.elapsedRealtime() - lastUpdateTime
            (rawPosition + (elapsed * speed).toLong()).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        } else {
            rawPosition.coerceAtLeast(0L)
        }

        val hasTrack = title.isNotBlank()
        val shouldDisplay: Boolean

        if (isPlaying && hasTrack) {
            Log.d("DI_MUSIC", "PlaybackState.STATE_PLAYING active trigger for ${controller.packageName}")
            pauseGracePeriodJob?.cancel()
            shouldDisplay = true
            startPlaybackTicker(controller)
        } else {
            playbackTickerJob?.cancel()
            if (hasTrack) {
                // If it was already displaying or just paused, keep active for 6 seconds grace period
                shouldDisplay = _musicState.value.isDisplayActive
                if (pauseGracePeriodJob == null || !pauseGracePeriodJob!!.isActive) {
                    pauseGracePeriodJob = helperScope.launch {
                        delay(6000)
                        if (!_musicState.value.isPlaying) {
                            Log.d("DI_MUSIC", "6s grace period ended, hiding music trigger")
                            _musicState.value = _musicState.value.copy(isDisplayActive = false)
                        }
                    }
                }
            } else {
                pauseGracePeriodJob?.cancel()
                shouldDisplay = false
            }
        }

        _musicState.value = MusicState(
            title = title,
            artist = artist,
            album = album,
            albumArt = albumArt,
            isPlaying = isPlaying,
            durationMs = if (duration > 0) duration else 0L,
            positionMs = currentPosition,
            packageName = controller.packageName,
            isDisplayActive = shouldDisplay,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun startPlaybackTicker(controller: MediaController) {
        playbackTickerJob?.cancel()
        playbackTickerJob = helperScope.launch {
            while (isActive) {
                delay(500) // 500ms update interval while playing
                val pbState = controller.playbackState ?: break
                if (pbState.state != PlaybackState.STATE_PLAYING) break

                val duration = _musicState.value.durationMs
                val lastUpdateTime = pbState.lastPositionUpdateTime
                val speed = pbState.playbackSpeed
                val elapsed = if (lastUpdateTime > 0) SystemClock.elapsedRealtime() - lastUpdateTime else 0L
                val updatedPos = (pbState.position + (elapsed * speed).toLong())
                    .coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)

                _musicState.value = _musicState.value.copy(positionMs = updatedPos)
            }
        }
    }
}