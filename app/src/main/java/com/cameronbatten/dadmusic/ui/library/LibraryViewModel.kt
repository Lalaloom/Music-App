package com.cameronbatten.dadmusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [LibraryScreen].
 *
 * Exposes the live song list from [SongSource], live playback state so the library
 * can render a persistent mini-player, and routes tap events to [PlaybackSession].
 *
 * ## PlaybackSession release — Option A
 *
 * [PlaybackSession]'s concrete impl ([com.cameronbatten.dadmusic.playback.PlaybackController])
 * is a `@Singleton`; its lifecycle is tied to the process, not to this ViewModel.
 * Cleanup occurs in [com.cameronbatten.dadmusic.playback.PlaybackService.onDestroy]
 * when the process is torn down.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    songSource: SongSource,
    private val playbackSession: PlaybackSession,
    private val songLookup: SongLookup,
) : ViewModel() {

    /** The live list of songs ordered by title ASC. */
    val songs: StateFlow<List<Song>> = songSource.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Live playback state — drives mini-player visibility and play/pause icon. */
    val playbackState: StateFlow<PlaybackState> = playbackSession.state()

    /** The [Song] currently loaded in the player, or null when idle. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSong: StateFlow<Song?> = playbackState
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

    /** Called when the user taps a song row. */
    fun onSongTapped(song: Song) {
        viewModelScope.launch {
            playbackSession.play(song)
        }
    }

    /** Toggle play/pause from the mini-player. */
    fun playPause() {
        if (playbackState.value.isPlaying) {
            playbackSession.pause()
        } else {
            playbackSession.resume()
        }
    }
}
