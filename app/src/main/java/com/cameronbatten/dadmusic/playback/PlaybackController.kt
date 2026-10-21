package com.cameronbatten.dadmusic.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cameronbatten.dadmusic.data.db.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "PlaybackController"
private const val POSITION_POLL_INTERVAL_MS = 500L

/**
 * App-side facade that wraps a [MediaController] bound to [PlaybackService].
 *
 * ## Connection
 * The [MediaController] is built asynchronously on first construction. Any call
 * to [play] suspends until the controller is connected (lazy, deferred resolution).
 *
 * ## State
 * A [Player.Listener] syncs session events into [_state]. A coroutine job polls
 * [Player.currentPosition] every [POSITION_POLL_INTERVAL_MS] ms while playing.
 *
 * ## Error handling (Review Focus #4)
 * [PlaybackException] thrown for an invalid or missing URI is caught in the
 * listener; state is updated with `isPlaying = false` and the error message is
 * logged. No crash propagation.
 *
 * ## Process death (Review Focus #5)
 * When the host process dies and is recreated, a new [PlaybackController] can
 * reconnect to the still-running [PlaybackService] (it runs in the same process
 * but its Binder is kept alive by the media notification). The controller
 * simply calls [MediaController.Builder.buildAsync] again; the session replays
 * its current [Player.State] to the fresh listener.
 *
 * @param context Application context — injected by Hilt.
 */
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackState())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionPollingJob: Job? = null

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val controller = mediaController ?: return
            _state.value = _state.value.copy(
                isPlaying = isPlaying,
                positionMs = controller.currentPosition,
                durationMs = controller.duration.coerceAtLeast(0L),
            )
            if (isPlaying) startPositionPolling() else stopPositionPolling()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val controller = mediaController ?: return
            val songId = mediaItem?.mediaId?.toLongOrNull()
            _state.value = _state.value.copy(
                currentSongId = songId,
                positionMs = controller.currentPosition,
                durationMs = controller.duration.coerceAtLeast(0L),
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            // Review Focus #4: invalid / missing URI must not crash
            Log.e(TAG, "Playback error: ${error.message}", error)
            _state.value = _state.value.copy(
                isPlaying = false,
                lastError = error.message,
            )
            stopPositionPolling()
        }
    }

    init {
        connectToService()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Continuous stream of playback state. */
    override fun state(): StateFlow<PlaybackState> = _state.asStateFlow()

    /**
     * Load and play [song]. Suspends until the [MediaController] is connected.
     * Encodes [Song.id] as the [MediaItem.mediaId] so [playerListener] can
     * round-trip the song identity back to [PlaybackState.currentSongId].
     *
     * Pre-playback failures (service not bound, controller connection error) are
     * caught here so callers (e.g. ViewModel) never see a thrown exception.
     * Playback-time failures (bad URI, decode error) arrive asynchronously via
     * [Player.Listener.onPlayerError] — see [playerListener].
     */
    override suspend fun play(song: Song) {
        try {
            val controller = awaitController()
            val mediaItem = MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .build()
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        } catch (e: CancellationException) {
            throw e // never swallow cancellation
        } catch (e: Exception) {
            Log.w(TAG, "play() failed before playback could start", e)
            _state.update {
                it.copy(
                    isPlaying = false,
                    lastError = e.message ?: "Playback could not start",
                )
            }
        }
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun resume() {
        mediaController?.play()
    }

    override fun seekTo(ms: Long) {
        mediaController?.seekTo(ms)
    }

    override fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    override fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    /**
     * Returns the underlying [Player] for video-mode UI attachment.
     *
     * Returns null if the [MediaController] is not yet connected or has been released.
     * The UI must not hold this reference beyond the owning Composable's lifetime.
     */
    override fun getPlayer(): Player? = mediaController

    /**
     * Release the controller and cancel the coroutine scope.
     * Must be called from the component that owns this controller (e.g. ViewModel.onCleared).
     */
    fun release() {
        stopPositionPolling()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        scope.cancel()
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun connectToService() {
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    // Sync initial state from session (handles rebind after process death)
                    _state.value = PlaybackState(
                        currentSongId = controller.currentMediaItem?.mediaId?.toLongOrNull(),
                        isPlaying = controller.isPlaying,
                        positionMs = controller.currentPosition,
                        durationMs = controller.duration.coerceAtLeast(0L),
                    )
                    if (controller.isPlaying) startPositionPolling()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to PlaybackService", e)
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    /**
     * Suspends until [MediaController] is ready.  Used by [play] so callers
     * never race against the async connection.
     */
    private suspend fun awaitController(): MediaController {
        // Fast path: already connected
        mediaController?.let { return it }

        // Slow path: wait for the future to resolve
        return suspendCancellableCoroutine { continuation ->
            val future = controllerFuture
            if (future == null) {
                continuation.resumeWithException(
                    IllegalStateException("PlaybackController has been released"),
                )
                return@suspendCancellableCoroutine
            }
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun startPositionPolling() {
        stopPositionPolling()
        positionPollingJob = scope.launch {
            while (true) {
                delay(POSITION_POLL_INTERVAL_MS)
                val controller = mediaController ?: break
                if (!controller.isPlaying) break
                _state.value = _state.value.copy(positionMs = controller.currentPosition)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }
}
