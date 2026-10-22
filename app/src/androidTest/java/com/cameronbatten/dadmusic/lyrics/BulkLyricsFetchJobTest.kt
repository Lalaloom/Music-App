package com.cameronbatten.dadmusic.lyrics

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BulkLyricsFetchJobTest {

    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        songDao = db.songDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Fake fetcher ──────────────────────────────────────────────────────────

    private class StubFetcher(
        private val outcomes: MutableMap<Long, LyricsFetchResult> = mutableMapOf(),
    ) : SongLyricsFetcher {
        var invocationCount = 0
            private set

        fun stub(songId: Long, result: LyricsFetchResult) {
            outcomes[songId] = result
        }

        override suspend fun fetchFor(song: com.cameronbatten.dadmusic.data.db.Song): LyricsFetchResult {
            invocationCount++
            return outcomes[song.id] ?: LyricsFetchResult.OfflineOrFailed("unstubbed")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun insertSong(title: String, status: LyricsStatus = LyricsStatus.MISSING): Long {
        val id = songDao.insert(
            Song(
                title = title,
                artist = null,
                album = null,
                uri = "content://t/${title.hashCode()}",
                fileType = FileType.AUDIO,
                durationMs = 180_000L,
                dateAdded = 0L,
                confirmed = false,
                lyricsStatus = status,
            ),
        )
        return id
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun run_withNoMissingSongs_emitsSingleZeroZeroSnapshot() = runTest {
        insertSong("already", status = LyricsStatus.FETCHED_SYNCED)
        val job = BulkLyricsFetchJob(songDao, StubFetcher())

        val snapshots = job.run().toList()

        assertEquals(1, snapshots.size)
        assertEquals(FetchProgress(0, 0, 0), snapshots[0])
    }

    @Test
    fun run_withAllSuccess_progressReachesTotalWithZeroFailures() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val c = insertSong("c")
        val fetcher = StubFetcher().apply {
            stub(a, LyricsFetchResult.Fetched(hasTimings = true))
            stub(b, LyricsFetchResult.Fetched(hasTimings = true))
            stub(c, LyricsFetchResult.Fetched(hasTimings = false))
        }
        val job = BulkLyricsFetchJob(songDao, fetcher)

        val last = job.run().last()

        assertEquals(FetchProgress(completed = 3, total = 3, failed = 0), last)
        assertEquals(3, fetcher.invocationCount)
    }

    @Test
    fun run_halfFail_failCountMatches() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val c = insertSong("c")
        val d = insertSong("d")
        val fetcher = StubFetcher().apply {
            stub(a, LyricsFetchResult.Fetched(hasTimings = true))
            stub(b, LyricsFetchResult.NotFound)
            stub(c, LyricsFetchResult.Fetched(hasTimings = false))
            stub(d, LyricsFetchResult.OfflineOrFailed("boom"))
        }
        val job = BulkLyricsFetchJob(songDao, fetcher)

        val last = job.run().last()

        assertEquals(FetchProgress(completed = 4, total = 4, failed = 2), last)
    }

    @Test
    fun run_allOffline_everyAttemptCountsAsFailed() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val fetcher = StubFetcher().apply {
            stub(a, LyricsFetchResult.OfflineOrFailed("x"))
            stub(b, LyricsFetchResult.OfflineOrFailed("x"))
        }
        val job = BulkLyricsFetchJob(songDao, fetcher)

        val last = job.run().last()
        assertEquals(FetchProgress(2, 2, 2), last)
    }

    @Test
    fun run_firstEmission_isAlwaysZeroCompleted() = runTest {
        insertSong("a")
        insertSong("b")
        val fetcher = StubFetcher()

        val first = job(fetcher).run().first()

        assertEquals(0, first.completed)
        assertEquals(2, first.total)
        assertEquals(0, first.failed)
    }

    @Test
    fun run_skipsSongsWithStatusOtherThanMissing() = runTest {
        insertSong("sync", status = LyricsStatus.FETCHED_SYNCED)
        insertSong("fail", status = LyricsStatus.FAILED)
        insertSong("adopt", status = LyricsStatus.ADOPTED_FROM_DISK)
        val target = insertSong("miss")
        val fetcher = StubFetcher().apply {
            stub(target, LyricsFetchResult.Fetched(hasTimings = true))
        }
        val job = BulkLyricsFetchJob(songDao, fetcher)

        val last = job.run().last()

        assertEquals(FetchProgress(1, 1, 0), last)
        assertEquals("only MISSING songs should be fetched", 1, fetcher.invocationCount)
    }

    private fun job(fetcher: SongLyricsFetcher) = BulkLyricsFetchJob(songDao, fetcher)
}
