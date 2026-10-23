package com.cameronbatten.dadmusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.playback.PlaybackSession
import com.cameronbatten.dadmusic.playback.PlaybackState
import com.cameronbatten.dadmusic.playback.SongLookup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for [PlayerScreen].
 *
 * Exposes [state] (live [PlaybackState]) and [currentSong] (the [Song] entity
 * currently loaded, looked up by id from [SongLookup]).
 *
 * ## Option A — no release()
 * [PlaybackSession] is a singleton; this ViewModel does NOT call release() on
 * cleared — lifecycle is owned at the process level (PlaybackService.onDestroy).
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackSession: PlaybackSession,
    private val songLookup: SongLookup,
) : ViewModel() {

    /** Live playback state from the session. */
    val state: StateFlow<PlaybackState> = playbackSession.state()

    /**
     * The [Song] entity corresponding to [PlaybackState.currentSongId], or null
     * when idle or the id cannot be resolved.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSong: StateFlow<Song?> = state
        .flatMapLatest { ps ->
            flow {
                emit(ps.currentSongId?.let { songLookup.findById(it) })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** Toggle play/pause depending on current [PlaybackState.isPlaying]. */
    fun playPause() {
        if (state.value.isPlaying) {
            playbackSession.pause()
        } else {
            playbackSession.resume()
        }
    }

    fun next() {
        playbackSession.skipNext()
    }

    fun previous() {
        playbackSession.skipPrevious()
    }

    fun seek(ms: Long) {
        playbackSession.seekTo(ms)
    }

    /**
     * Exposes the underlying [Player] for video-mode [androidx.media3.ui.PlayerView]
     * attachment. Delegates to [PlaybackSession.getPlayer]; returns null if not yet
     * connected or released.
     */
    fun getPlayer(): Player? = playbackSession.getPlayer()
}
