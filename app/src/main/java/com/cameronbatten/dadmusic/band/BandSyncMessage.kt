package com.cameronbatten.dadmusic.band

/**
 * Wire-level messages exchanged between leader and followers in band mode.
 *
 * Encoded as single-line JSON: `{"kind":"play","songUri":"...","positionMs":123}\n`.
 * One message per line keeps the stream parseable without a framing layer.
 */
sealed interface BandSyncMessage {

    /** Leader → follower: play / seek to this song at this position. */
    data class Play(val songUri: String, val positionMs: Long) : BandSyncMessage

    /** Leader → follower: hold here (no song change). */
    data class Pause(val positionMs: Long) : BandSyncMessage

    /** Leader → follower: periodic heartbeat so a follower can trust the link. */
    data class Heartbeat(val epochMs: Long) : BandSyncMessage
}
