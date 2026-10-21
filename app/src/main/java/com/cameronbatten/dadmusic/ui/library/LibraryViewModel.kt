package com.cameronbatten.dadmusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [LibraryScreen].
 *
 * Exposes the live song list from [SongSource] and routes tap events to
 * [PlaybackCommands].
 *
 * ## PlaybackController release — Option A
 *
 * [PlaybackCommands] is a narrow interface that does NOT expose `release()`.
 * The concrete [com.cameronbatten.dadmusic.playback.PlaybackController] is a
 * `@Singleton`; its lifecycle is tied to the process, not to this ViewModel.
 * Cleanup occurs in [com.cameronbatten.dadmusic.playback.PlaybackService.onDestroy]
 * when the process is torn down.
 *
 * This means music continues playing through configuration changes and screen
 * navigation — the correct behaviour for a media app.  Two ViewModels (Library +
 * Player) can safely hold a reference to the same singleton without racing on
 * `release()`.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    songSource: SongSource,
    private val playbackCommands: PlaybackCommands,
) : ViewModel() {

    /**
     * The live list of songs ordered by title ASC.
     *
     * Exposed as a `val` so recomposition does not create a new upstream [stateIn]
     * subscription on every call.
     */
    val songs: StateFlow<List<Song>> = songSource.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Called when the user taps a song row. Launches playback on [viewModelScope]
     * so the caller does not need to await it.
     */
    fun onSongTapped(song: Song) {
        viewModelScope.launch {
            playbackCommands.play(song)
        }
    }
}
