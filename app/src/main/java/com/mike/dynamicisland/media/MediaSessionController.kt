package com.mike.dynamicisland.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.notifications.MikeNotificationListenerService
import com.mike.dynamicisland.utils.DeviceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contrôleur MediaSession natif Android (Module 07).
 * Lit uniquement les sessions multimédias réelles (aucune donnée inventée)
 * et redimensionne la pochette pour préserver la RAM sur 3 Go / Helio P22.
 */
class MediaSessionController(
    private val activityManager: MikeActivityManager,
    private val eventManager: EventManager,
    private val deviceProfileProvider: () -> DeviceProfile
) {
    private val _currentMedia = MutableStateFlow<IslandActivity.Music?>(null)
    val currentMedia: StateFlow<IslandActivity.Music?> = _currentMedia.asStateFlow()

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishFromController(activeController)
        }

        override fun onSessionDestroyed() {
            detachCurrentController()
            emitMedia(null)
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindBestController(controllers.orEmpty())
    }

    fun startListening(context: Context) {
        if (isListening) return
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return
        val listenerComponent = ComponentName(context, MikeNotificationListenerService::class.java)
        runCatching {
            val active = manager.getActiveSessions(listenerComponent)
            manager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent,
                mainHandler
            )
            sessionManager = manager
            isListening = true
            bindBestController(active)
            eventManager.record("Musique", "MediaSessionManager connecté (${active.size} session(s))", true)
        }.onFailure {
            eventManager.record("Musique", "Accès notifications requis pour lire MediaSession", true)
        }
    }

    fun stopListening() {
        if (!isListening) return
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        detachCurrentController()
        sessionManager = null
        isListening = false
        emitMedia(null)
    }

    fun togglePlayPause(): Boolean {
        val controller = activeController ?: return false
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        return runCatching {
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            eventManager.record("Musique", if (isPlaying) "Pause envoyée" else "Lecture envoyée", true)
            true
        }.getOrDefault(false)
    }

    fun skipToNext(): Boolean {
        val controller = activeController ?: return false
        return runCatching {
            controller.transportControls.skipToNext()
            eventManager.record("Musique", "Piste suivante demandée", true)
            true
        }.getOrDefault(false)
    }

    fun skipToPrevious(): Boolean {
        val controller = activeController ?: return false
        return runCatching {
            controller.transportControls.skipToPrevious()
            eventManager.record("Musique", "Piste précédente demandée", true)
            true
        }.getOrDefault(false)
    }

    private fun bindBestController(controllers: List<MediaController>) {
        val preferred = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED ||
                it.playbackState?.state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull()

        if (preferred?.sessionToken == activeController?.sessionToken && preferred != null) {
            publishFromController(preferred)
            return
        }

        detachCurrentController()
        if (preferred == null) {
            emitMedia(null)
            return
        }

        activeController = preferred
        runCatching { preferred.registerCallback(controllerCallback, mainHandler) }
        publishFromController(preferred)
    }

    private fun detachCurrentController() {
        activeController?.let { controller ->
            runCatching { controller.unregisterCallback(controllerCallback) }
        }
        activeController = null
    }

    private fun publishFromController(controller: MediaController?) {
        if (controller == null) {
            emitMedia(null)
            return
        }
        val state = controller.playbackState
        val metadata = controller.metadata
        val isActiveState = state != null && (
            state.state == PlaybackState.STATE_PLAYING ||
                state.state == PlaybackState.STATE_PAUSED ||
                state.state == PlaybackState.STATE_BUFFERING
            )

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().trim()
        val artist = (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty().trim()

        if (!isActiveState && title.isBlank()) {
            emitMedia(null)
            return
        }

        val rawArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val scaledArt = downscaleBitmapForRam(rawArt, deviceProfileProvider().maxAlbumArtSizePx)

        val musicActivity = IslandActivity.Music(
            packageName = controller.packageName.orEmpty(),
            title = title.ifBlank { "Lecture audio" },
            artist = artist.ifBlank { controller.packageName.substringAfterLast('.') },
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            positionMs = state?.position?.coerceAtLeast(0L) ?: 0L,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L,
            artwork = scaledArt
        )
        emitMedia(musicActivity)
    }

    private fun downscaleBitmapForRam(source: Bitmap?, maxDimPx: Int): Bitmap? {
        if (source == null || source.isRecycled) return null
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= maxDimPx) return source
        val scale = maxDimPx.toFloat() / maxSide.toFloat()
        val targetW = (source.width * scale).toInt().coerceAtLeast(1)
        val targetH = (source.height * scale).toInt().coerceAtLeast(1)
        return runCatching { Bitmap.createScaledBitmap(source, targetW, targetH, true) }
            .getOrDefault(source)
    }

    private fun emitMedia(music: IslandActivity.Music?) {
        _currentMedia.value = music
        if (music != null) {
            activityManager.upsert(music)
        } else {
            activityManager.removeByKind(ActivityKind.MUSIC)
        }
    }
}
