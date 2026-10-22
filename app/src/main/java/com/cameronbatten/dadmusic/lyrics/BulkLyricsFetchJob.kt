package com.cameronbatten.dadmusic.lyrics

import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progress snapshot emitted while a bulk fetch is in flight.
 *
 * @param completed Count of songs the orchestrator has finished processing — succeeded or failed.
 * @param total     Snapshot of the candidate count taken at the start of the run.
 * @param failed    Count of candidates that produced a non-[LyricsFetchResult.Fetched] outcome.
 */
data class FetchProgress(val completed: Int, val total: Int, val failed: Int)

/**
 * Walks every song whose `lyricsStatus == MISSING`, hands each one to the injected
 * [SongLyricsFetcher], and emits a [FetchProgress] after each attempt plus one
 * initial snapshot at `(0, total, 0)` so collectors can wire up progress bars
 * before the first fetch resolves.
 *
 * Cancellation: the body cooperates via [currentCoroutineContext] + [ensureActive]
 * so a collector that cancels its scope immediately stops the walk; in-flight
 * HTTP calls still get the usual Retrofit-level cancellation via OkHttp.
 */
@Singleton
class BulkLyricsFetchJob @Inject constructor(
    private val songDao: SongDao,
    private val fetcher: SongLyricsFetcher,
) {

    fun run(): Flow<FetchProgress> = flow {
        val candidates = songDao.getAll().filter { it.lyricsStatus == LyricsStatus.MISSING }
        val total = candidates.size
        var completed = 0
        var failed = 0

        emit(FetchProgress(completed = 0, total = total, failed = 0))

        for (song in candidates) {
            currentCoroutineContext().ensureActive()
            val result = fetcher.fetchFor(song)
            if (result !is LyricsFetchResult.Fetched) {
                failed++
            }
            completed++
            emit(FetchProgress(completed = completed, total = total, failed = failed))
        }
    }
}
