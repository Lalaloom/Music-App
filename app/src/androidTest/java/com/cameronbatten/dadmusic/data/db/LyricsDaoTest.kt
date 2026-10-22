package com.cameronbatten.dadmusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricsDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao
    private lateinit var dao: LyricsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        songDao = db.songDao()
        dao = db.lyricsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertSong(uri: String = "content://test/song.mp3"): Long {
        val song = Song(
            title = "Test",
            artist = "Artist",
            album = null,
            uri = uri,
            fileType = FileType.AUDIO,
            durationMs = 180_000L,
            dateAdded = 0L,
            confirmed = true,
        )
        return songDao.insert(song)
    }

    private fun lyrics(
        songId: Long,
        rawLrc: String = "[00:00.00]line\n",
        hasTimings: Boolean = true,
        source: LyricsSource = LyricsSource.LRCLIB,
    ) = Lyrics(
        songId = songId,
        rawLrc = rawLrc,
        hasTimings = hasTimings,
        source = source,
        modifiedUtc = 1_700_000_000L,
    )

    @Test
    fun upsert_insertsNewLyrics() = runTest {
        val songId = insertSong()
        val id = dao.upsert(lyrics(songId))
        assertTrue("id must be assigned", id > 0)

        val found = dao.findBySongId(songId)
        assertNotNull(found)
        assertEquals(songId, found!!.songId)
        assertEquals(LyricsSource.LRCLIB, found.source)
    }

    @Test
    fun upsert_replacesLyricsForSameSongId() = runTest {
        val songId = insertSong()
        dao.upsert(lyrics(songId, rawLrc = "first", hasTimings = false))
        dao.upsert(lyrics(songId, rawLrc = "second", hasTimings = true, source = LyricsSource.USER_EDIT))

        val found = dao.findBySongId(songId)
        assertNotNull(found)
        assertEquals("second", found!!.rawLrc)
        assertTrue(found.hasTimings)
        assertEquals(LyricsSource.USER_EDIT, found.source)
    }

    @Test
    fun findBySongId_returnsNullWhenMissing() = runTest {
        val songId = insertSong()
        assertNull(dao.findBySongId(songId))
    }

    @Test
    fun observeBySongId_emitsUpdatesOnUpsert() = runTest {
        val songId = insertSong()
        dao.upsert(lyrics(songId))
        val observed = dao.observeBySongId(songId).first()
        assertNotNull(observed)
        assertEquals(songId, observed!!.songId)
    }

    @Test
    fun deleteBySongId_removesRow() = runTest {
        val songId = insertSong()
        dao.upsert(lyrics(songId))
        dao.deleteBySongId(songId)
        assertNull(dao.findBySongId(songId))
    }

    @Test
    fun deletingSong_cascadesToLyrics() = runTest {
        val uri = "content://test/cascade.mp3"
        val songId = insertSong(uri)
        dao.upsert(lyrics(songId))

        songDao.deleteByUri(uri)

        assertNull(dao.findBySongId(songId))
    }
}
