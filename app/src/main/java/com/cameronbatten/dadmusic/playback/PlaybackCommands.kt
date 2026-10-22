package com.cameronbatten.dadmusic.playback

import com.cameronbatten.dadmusic.data.db.Song

/**
 * Narrow interface for triggering song playback.
 *
 * [PlaybackController] implements this interface.
 * Tests inject a fake instead.
 *
 * Note: `release()` is intentionally excluded from this interface — ownership of
 * [PlaybackController] cleanup lives at the process level (see Option A decision).
 */
interface PlaybackCommands {
    /**
     * Load and play [song]. This is a suspend function so callers can launch it
     * on [androidx.lifecycle.ViewModel.viewModelScope].
     */
    suspend fun play(song: Song)
}
