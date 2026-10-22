package com.cameronbatten.dadmusic.setlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.LiveQueueDao
import com.cameronbatten.dadmusic.data.db.LiveQueueEntry
import com.cameronbatten.dadmusic.data.db.QueueSource
import com.cameronbatten.dadmusic.data.db.Setlist
import com.cameronbatten.dadmusic.data.db.SetlistDao
import com.cameronbatten.dadmusic.data.db.SetlistEntry
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveQueueManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao
    private lateinit var setlistDao: SetlistDao
    private lateinit var liveQueueDao: LiveQueueDao
    private lateinit var manager: LiveQueueManager

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        songDao = db.songDao()
        setlistDao = db.setlistDao()
        liveQueueDao = db.liveQueueDao()
        manager = LiveQueueManager(liveQueueDao, setlistDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertSong(title: String): Long = songDao.insert(
        Song(
            title = title,
            artist = null,
            album = null,
            uri = "u/${title.hashCode()}",
            fileType = FileType.AUDIO,
            durationMs = 180_000L,
            dateAdded = 0L,
            confirmed = false,
        ),
    )

    private suspend fun createSetlist(name: String, songIds: List<Long>): Long {
        val id = setlistDao.upsert(
            Setlist(
                name = name,
                sourceFolderUri = "folder/$name",
                orderIndex = 0,
                createdAt = 0L,
                modifiedUtc = 0L,
            ),
        )
        setlistDao.insertEntries(
            songIds.mapIndexed { i, sid ->
                SetlistEntry(setlistId = id, songId = sid, position = (i + 1) * 1000)
            },
        )
        return id
    }

    @Test
    fun openSetlist_wipesPriorQueueAndCopiesEntriesInOrder() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val c = insertSong("c")
        val setlistId = createSetlist("Set 1", listOf(a, b, c))

        // Prime the queue with something irrelevant first.
        liveQueueDao.insert(
            LiveQueueEntry(position = 100, songId = a, source = QueueSource.REQUEST, parentSetlistId = null),
        )

        manager.openSetlist(setlistId)

        val snap = manager.snapshot()
        assertEquals(3, snap.size)
        assertEquals(listOf(a, b, c), snap.map { it.songId })
        assertEquals(listOf(1000, 2000, 3000), snap.map { it.position })
        assertTrue(snap.all { it.source == QueueSource.SAVED })
        assertTrue(snap.all { it.parentSetlistId == setlistId })
    }

    @Test
    fun append_lands_atEndWithSparseStep() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val setlistId = createSetlist("S", listOf(a, b))
        manager.openSetlist(setlistId)

        val c = insertSong("c")
        val appended = manager.append(c, QueueSource.REQUEST)

        assertEquals(3000, appended.position)
        val snap = manager.snapshot()
        assertEquals(QueueSource.REQUEST, snap.last().source)
        assertEquals(setlistId, snap.last().parentSetlistId)
    }

    @Test
    fun append_withoutParent_leavesParentNull() = runTest {
        val a = insertSong("a")

        manager.append(a, QueueSource.REQUEST)

        val snap = manager.snapshot()
        assertEquals(1, snap.size)
        assertNull(snap.first().parentSetlistId)
    }

    @Test
    fun remove_dropsSpecifiedPosition() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val setlistId = createSetlist("S", listOf(a, b))
        manager.openSetlist(setlistId)

        manager.remove(position = 1000)

        val snap = manager.snapshot()
        assertEquals(1, snap.size)
        assertEquals(b, snap.first().songId)
    }

    @Test
    fun moveByIndex_renumbersWithFreshSparsePositions() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val c = insertSong("c")
        val setlistId = createSetlist("S", listOf(a, b, c))
        manager.openSetlist(setlistId)

        // Move "a" (index 0) to the end (index 2).
        manager.moveByIndex(fromIndex = 0, toIndex = 2)

        val snap = manager.snapshot()
        assertEquals(listOf(b, c, a), snap.map { it.songId })
        assertEquals(listOf(1000, 2000, 3000), snap.map { it.position })
    }

    @Test
    fun queueSurvivesManagerRebind_simulatingProcessDeath() = runTest {
        // Review Focus #3: process-death during a gig.
        val a = insertSong("a")
        val b = insertSong("b")
        val setlistId = createSetlist("S", listOf(a, b))
        manager.openSetlist(setlistId)
        val c = insertSong("c")
        manager.append(c, QueueSource.REQUEST)

        // Discard the manager; create a fresh one on the same DB.
        val rebound = LiveQueueManager(liveQueueDao, setlistDao)
        val snap = rebound.snapshot()

        assertEquals(listOf(a, b, c), snap.map { it.songId })
        assertEquals(setlistId, rebound.currentParentSetlistId())
    }

    @Test
    fun hasDivergedFromParent_falseWhenQueueMatchesSaved() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val setlistId = createSetlist("S", listOf(a, b))
        manager.openSetlist(setlistId)

        assertFalse(manager.hasDivergedFromParent())
    }

    @Test
    fun hasDivergedFromParent_trueAfterRequestAdd() = runTest {
        val a = insertSong("a")
        val setlistId = createSetlist("S", listOf(a))
        manager.openSetlist(setlistId)
        val b = insertSong("b")
        manager.append(b, QueueSource.REQUEST)

        assertTrue(manager.hasDivergedFromParent())
    }

    @Test
    fun hasDivergedFromParent_trueAfterMove() = runTest {
        val a = insertSong("a")
        val b = insertSong("b")
        val setlistId = createSetlist("S", listOf(a, b))
        manager.openSetlist(setlistId)

        manager.moveByIndex(0, 1)

        assertTrue(manager.hasDivergedFromParent())
    }

    @Test
    fun hasDivergedFromParent_falseInLibraryViewMode() = runTest {
        val a = insertSong("a")
        manager.append(a, QueueSource.REQUEST) // no parent

        assertFalse(manager.hasDivergedFromParent())
    }
}
