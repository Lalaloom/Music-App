package com.cameronbatten.dadmusic.lyrics.net

import kotlinx.serialization.Serializable

/**
 * Response from `GET https://lrclib.net/api/get`.
 *
 * LRCLIB may omit any string field when a match is partial, so every text field
 * is nullable. `instrumental = true` means the API confirmed there are no lyrics
 * for this track (not a fetch failure) — callers should treat it as `NotFound`.
 *
 * The `unknownKeys` tolerance on the shared [kotlinx.serialization.json.Json]
 * instance (see `NetworkModule.provideJson`) lets us absorb future additions to
 * the LRCLIB payload without a client upgrade.
 */
@Serializable
data class LrclibGetResponse(
    val id: Long,
    val trackName: String,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)
