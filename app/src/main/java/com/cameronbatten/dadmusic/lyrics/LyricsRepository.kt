package com.cameronbatten.dadmusic.lyrics

import com.cameronbatten.dadmusic.data.db.Lyrics
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.LyricsSource
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.lyrics.net.LrclibApi
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches lyrics for a single song against LRCLIB and persists the result:
 *   - cache file via [LrcCacheStore]
 *   - `Lyrics` row via [LyricsDao]
 *   - updated `lyricsStatus` + `lyricsCachePath` on the `Song` row via [SongDao]
 *
 * All failure modes (404, non-2xx, IO, malformed JSON) are mapped onto
 * [LyricsFetchResult] so callers never need to catch Retrofit types. The song's
 * `lyricsStatus` is always transitioned so a UI badge can reflect reality.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val api: LrclibApi,
    private val cacheStore: LrcCacheStore,
    private val lyricsDao: LyricsDao,
    private val songDao: SongDao,
) : SongLyricsFetcher {

    override suspend fun fetchFor(song: Song): LyricsFetchResult {
        songDao.upsert(song.copy(lyricsStatus = LyricsStatus.FETCHING))

        val response = try {
            val durationSec = if (song.durationMs > 0) song.durationMs / 1000.0 else null
            api.get(
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationSec = durationSec,
            )
        } catch (e: HttpException) {
            return if (e.code() == 404) {
                markFailed(song)
                LyricsFetchResult.NotFound
            } else {
                markFailed(song)
                LyricsFetchResult.OfflineOrFailed("HTTP ${e.code()}")
            }
        } catch (e: IOException) {
            markFailed(song)
            return LyricsFetchResult.OfflineOrFailed("Network unavailable")
        } catch (e: SerializationException) {
            markFailed(song)
            return LyricsFetchResult.OfflineOrFailed("Malformed response")
        }

        // Instrumental or no lyric text at all → treat as NotFound.
        if (response.instrumental || (response.syncedLyrics == null && response.plainLyrics == null)) {
            markFailed(song)
            return LyricsFetchResult.NotFound
        }

        val lrcText = response.syncedLyrics ?: response.plainLyrics!!
        val hasTimings = response.syncedLyrics != null

        val cachePath = try {
            cacheStore.write(song.id, lrcText)
        } catch (e: IOException) {
            markFailed(song)
            return LyricsFetchResult.OfflineOrFailed("Could not write cache: ${e.message}")
        }

        lyricsDao.upsert(
            Lyrics(
                songId = song.id,
                rawLrc = lrcText,
                hasTimings = hasTimings,
                source = LyricsSource.LRCLIB,
                modifiedUtc = System.currentTimeMillis(),
            ),
        )

        songDao.upsert(
            song.copy(
                lyricsStatus = if (hasTimings) LyricsStatus.FETCHED_SYNCED else LyricsStatus.FETCHED_UNSYNCED,
                lyricsCachePath = cachePath,
            ),
        )

        return LyricsFetchResult.Fetched(hasTimings)
    }

    private suspend fun markFailed(song: Song) {
        songDao.upsert(song.copy(lyricsStatus = LyricsStatus.FAILED))
    }
}
