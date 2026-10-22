package com.cameronbatten.dadmusic.playback

/**
 * Immutable snapshot of the current playback state, emitted by [PlaybackController.state].
 *
 * @param currentSongId  DB id of the song currently loaded, or null when idle.
 * @param isPlaying      True when the player is actively producing audio/video.
 * @param positionMs     Current playback position in milliseconds.
 * @param durationMs     Total duration of the current media in milliseconds.
 * @param lastError      Human-readable description of the last playback error, or null.
 */
data class PlaybackState(
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastError: String? = null,
)
