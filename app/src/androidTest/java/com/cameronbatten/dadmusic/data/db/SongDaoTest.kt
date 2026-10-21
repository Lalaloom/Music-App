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
class SongDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SongDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.songDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun song(
        title: String = "Test Song",
        uri: String = "content://test/$title",
    ) = Song(
        title = title,
        artist = "Artist",
        album = "Album",
        uri = uri,
        fileType = FileType.AUDIO,
        durationMs = 180_000L,
        dateAdded = System.currentTimeMillis(),
        confirmed = true,
    )

    @Test
    fun insert_assignsAutoId() = runTest {
        val inserted = song(title = "Track A", uri = "content://test/a")
        val id = dao.insert(inserted)
        assertTrue("id should be > 0", id > 0)
    }

    @Test
    fun insertAll_persistsEveryRow() = runTest {
        val songs = listOf(
            song(title = "Alpha", uri = "content://test/alpha"),
            song(title = "Beta", uri = "content://test/beta"),
            song(title = "Gamma", uri = "content://test/gamma"),
        )
        val ids = dao.insertAll(songs)
        assertEquals(3, ids.size)
        val all = dao.observeAll().first()
        assertEquals(3, all.size)
    }

    @Test
    fun observeAll_emitsSortedByTitle() = runTest {
        dao.insert(song(title = "Zebra", uri = "content://test/z"))
        dao.insert(song(title = "Apple", uri = "content://test/a"))
        dao.insert(song(title = "Mango", uri = "content://test/m"))

        val all = dao.observeAll().first()
        assertEquals(listOf("Apple", "Mango", "Zebra"), all.map { it.title })
    }

    @Test
    fun findByUri_returnsNullWhenMissing() = runTest {
        val result = dao.findByUri("content://test/nonexistent")
        assertNull(result)
    }

    @Test
    fun deleteByUri_removesRowAndFutureObservationsExcludeIt() = runTest {
        val uri = "content://test/delete-me"
        dao.insert(song(title = "Delete Me", uri = uri))

        val before = dao.observeAll().first()
        assertNotNull(before.firstOrNull { it.uri == uri })

        dao.deleteByUri(uri)

        val after = dao.observeAll().first()
        assertNull(after.firstOrNull { it.uri == uri })
    }
}
