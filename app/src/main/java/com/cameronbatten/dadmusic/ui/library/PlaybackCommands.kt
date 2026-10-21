package com.cameronbatten.dadmusic.ui.library

import com.cameronbatten.dadmusic.data.db.Song

/**
 * Narrow interface for triggering song playback.
 *
 * [com.cameronbatten.dadmusic.playback.PlaybackController] implements this interface.
 * Tests inject a [FakePlaybackCommands] instead.
 *
 * Note: `release()` is intentionally excluded from this interface — ownership of
 * [PlaybackController] cleanup lives at the process level (see Option A decision
 * documented in [LibraryViewModel]).
 */
interface PlaybackCommands {
    /**
     * Load and play [song]. This is a suspend function so callers can launch it
     * on [androidx.lifecycle.ViewModel.viewModelScope].
     */
    suspend fun play(song: Song)
}
