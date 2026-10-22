package com.cameronbatten.dadmusic.playback

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits the underlying [androidx.media3.common.Player]'s `currentPosition` at
 * ~20 Hz while playing. 20 Hz (50 ms) is more than enough for lyric scrolling
 * — LRC timing accuracy is spec'd at ~100 ms (§7.5). When not playing we fall
 * back to a 500 ms heartbeat so the Zen view stays reactive without burning
 * cycles.
 */
@Singleton
class PositionTicker @Inject constructor(
    private val session: PlaybackSession,
) {
    fun ticks(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            val player = session.getPlayer()
            emit(player?.currentPosition ?: 0L)
            val playing = player?.isPlaying == true
            delay(if (playing) ACTIVE_POLL_MS else IDLE_POLL_MS)
        }
    }

    private companion object {
        const val ACTIVE_POLL_MS = 50L
        const val IDLE_POLL_MS = 500L
    }
}
