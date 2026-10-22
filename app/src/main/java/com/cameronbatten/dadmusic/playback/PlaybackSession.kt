package com.cameronbatten.dadmusic.playback

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

/**
 * Broader playback interface that extends [PlaybackCommands] with state observation
 * and transport controls, used by [com.cameronbatten.dadmusic.ui.player.PlayerViewModel].
 *
 * Having this as a separate interface from [PlaybackCommands] keeps [LibraryViewModel]'s
 * dependency narrow while giving [PlayerViewModel] everything it needs.
 *
 * [PlaybackController] implements this interface. Tests inject a [FakePlaybackSession].
 */
interface PlaybackSession : PlaybackCommands {
    /** Continuous stream of playback state. */
    fun state(): StateFlow<PlaybackState>

    fun pause()
    fun resume()
    fun seekTo(ms: Long)
    fun skipNext()
    fun skipPrevious()

    /**
     * Returns the underlying [Player] so a video-mode UI can attach it to a
     * [androidx.media3.ui.PlayerView].
     *
     * Returns null if the controller is not yet connected or has been released.
     * Callers must not hold this reference beyond the owning composable's lifetime.
     */
    fun getPlayer(): Player?
}
