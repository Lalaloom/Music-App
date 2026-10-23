package com.cameronbatten.dadmusic.lyrics.net

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Minimal Retrofit interface for the LRCLIB `/api/get` endpoint.
 *
 * LRCLIB docs: https://lrclib.net/docs
 *
 * - Returns `HTTP 200` with [LrclibGetResponse] on match (which may still contain
 *   null `syncedLyrics` / `plainLyrics` if LRCLIB has no text for the track).
 * - Returns `HTTP 404` when no record is found — Retrofit maps that to
 *   `retrofit2.HttpException` at the suspend call site.
 * - Malformed JSON throws `kotlinx.serialization.SerializationException`.
 * - Network / timeout failures surface as `java.io.IOException`.
 *
 * Callers (see `LyricsRepository`, Task 6) map all three failure families onto a
 * single sealed result type so the UI never has to know Retrofit is in use.
 */
interface LrclibApi {

    @GET("/api/get")
    suspend fun get(
        @Query("track_name") title: String,
        @Query("artist_name") artist: String?,
        @Query("album_name") album: String?,
        @Query("duration") durationSec: Double?,
    ): LrclibGetResponse
}
