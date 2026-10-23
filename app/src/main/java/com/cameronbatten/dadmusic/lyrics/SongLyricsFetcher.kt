package com.cameronbatten.dadmusic.lyrics

import com.cameronbatten.dadmusic.data.db.Song

/**
 * Narrow interface the bulk-fetch orchestrator depends on so instrumented tests
 * can inject a deterministic fake instead of standing up a real HTTP client.
 *
 * Concrete implementation: [LyricsRepository].
 */
interface SongLyricsFetcher {
    suspend fun fetchFor(song: Song): LyricsFetchResult
}
