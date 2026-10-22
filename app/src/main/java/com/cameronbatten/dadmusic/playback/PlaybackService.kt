package com.cameronbatten.dadmusic.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Media3 [MediaSessionService] that hosts an [ExoPlayer] as the session player.
 *
 * Audio-focus handling:
 * - [AudioAttributes] with [C.CONTENT_TYPE_MUSIC] and [C.USAGE_MEDIA].
 * - `handleAudioBecomingNoisy = true` so headphone removal auto-pauses (Review Focus #2).
 * - `handleAudioFocus = true` in [ExoPlayer.setAudioAttributes] so the OS manages focus.
 *
 * Lifecycle:
 * - [onCreate] builds player + session.
 * - [onGetSession] returns the session to connecting [MediaController] clients.
 * - [onDestroy] releases both to prevent leaks (Review Focus #5).
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                setHandleAudioBecomingNoisy(true)
                setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
