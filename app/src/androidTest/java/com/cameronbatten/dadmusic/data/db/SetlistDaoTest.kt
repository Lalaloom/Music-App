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
class SetlistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao
    private lateinit var dao: SetlistDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        songDao = db.songDao()
        dao = db.setlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun song(title: String, uri: String): Long = songDao.insert(
        Song(
            title = title,
            artist = null,
            album = null,
            uri = uri,
            fileType = FileType.AUDIO,
            durationMs = 180_000L,
            dateAdded = 0L,
            confirmed = false,
        ),
    )

    private fun setlist(
        name: String,
        folderUri: String? = null,
        orderIndex: Int = 0,
        createdAt: Long = 0L,
    ) = Setlist(
        name = name,
        sourceFolderUri = folderUri,
        orderIndex = orderIndex,
        createdAt = createdAt,
        modifiedUtc = 0L,
    )

    @Test
    fun upsert_assignsAutoIdAndFindByFolderRoundTrips() = runTest {
        val id = dao.upsert(setlist("Set 1", folderUri = "content://folder/1"))
        assertTrue(id > 0)
        val found = dao.findByFolder("content://folder/1")
        assertNotNull(found)
        assertEquals("Set 1", found!!.name)
    }

    @Test
    fun observeAll_sortedByOrderIndexThenCreatedAt() = runTest {
        dao.upsert(setlist("B", folderUri = "b", orderIndex = 0, createdAt = 2L))
        dao.upsert(setlist("A", folderUri = "a", orderIndex = 0, createdAt = 1L))
        dao.upsert(setlist("C", folderUri = "c", orderIndex = 1, createdAt = 0L))

        val all = dao.observeAll().first()
        assertEquals(listOf("A", "B", "C"), all.map { it.name })
    }

    @Test
    fun entriesFor_sortedByPositionAscAndIncludesSongFields() = runTest {
        val setlistId = dao.upsert(setlist("X", folderUri = "x"))
        val songA = song("alpha", "u/a")
        val songB = song("beta", "u/b")
        val songC = song("gamma", "u/c")
        dao.insertEntries(
            listOf(
                SetlistEntry(setlistId, songB, position = 2000),
                SetlistEntry(setlistId, songA, position = 1000),
                SetlistEntry(setlistId, songC, position = 3000),
            ),
        )

        val rows = dao.entriesFor(setlistId)
        assertEquals(listOf("alpha", "beta", "gamma"), rows.map { it.song.title })
        assertEquals(listOf(1000, 2000, 3000), rows.map { it.position })
    }

    @Test
    fun replaceEntries_isTransactional_removesStaleRows() = runTest {
        val setlistId = dao.upsert(setlist("X", folderUri = "x"))
        val songA = song("alpha", "u/a")
        val songB = song("beta", "u/b")

        dao.insertEntries(
            listOf(
                SetlistEntry(setlistId, songA, position = 1000),
                SetlistEntry(setlistId, songB, position = 2000),
            ),
        )
        assertEquals(2, dao.entriesFor(setlistId).size)

        // Replace with just A at a new position
        dao.replaceEntries(setlistId, listOf(SetlistEntry(setlistId, songA, position = 1000)))

        val after = dao.entriesFor(setlistId)
        assertEquals(1, after.size)
        assertEquals("alpha", after.first().song.title)
    }

    @Test
    fun deleteSetlist_cascadesToEntries() = runTest {
        val setlistId = dao.upsert(setlist("X", folderUri = "x"))
        val songA = song("alpha", "u/a")
        dao.insertEntries(listOf(SetlistEntry(setlistId, songA, position = 1000)))
        assertEquals(1, dao.entriesFor(setlistId).size)

        dao.deleteById(setlistId)

        assertEquals(0, dao.entriesFor(setlistId).size)
        assertNull(dao.findById(setlistId))
    }

    @Test
    fun deletingSong_cascadesToSetlistEntries() = runTest {
        val setlistId = dao.upsert(setlist("X", folderUri = "x"))
        val songA = song("alpha", "u/a")
        dao.insertEntries(listOf(SetlistEntry(setlistId, songA, position = 1000)))

        songDao.deleteByUri("u/a")

        assertEquals(0, dao.entriesFor(setlistId).size)
    }
}
