package com.cameronbatten.dadmusic.lyrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LrcCacheStoreTest {

    private lateinit var context: Context
    private lateinit var store: LrcCacheStore
    private lateinit var lyricsDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        lyricsDir = File(context.filesDir, "lyrics")
        lyricsDir.deleteRecursively()
        store = LrcCacheStore(context)
    }

    @After
    fun tearDown() {
        lyricsDir.deleteRecursively()
    }

    @Test
    fun write_then_read_roundTripsContent() = runTest {
        val lrc = "[00:12.00]Hello world"
        store.write(songId = 1L, lrcText = lrc)
        assertEquals(lrc, store.read(1L))
    }

    @Test
    fun write_overwritesExistingContent() = runTest {
        store.write(songId = 7L, lrcText = "first")
        store.write(songId = 7L, lrcText = "second")
        assertEquals("second", store.read(7L))
    }

    @Test
    fun read_returnsNullWhenFileMissing() = runTest {
        assertNull(store.read(songId = 42L))
    }

    @Test
    fun delete_isIdempotent() = runTest {
        store.delete(songId = 99L) // nothing there — must not throw
        store.write(songId = 99L, lrcText = "x")
        assertTrue(store.delete(99L))
        assertFalse("second delete returns false (nothing to remove)", store.delete(99L))
        assertNull(store.read(99L))
    }

    @Test
    fun write_leavesNoTempFileAfterSuccess() = runTest {
        store.write(songId = 3L, lrcText = "ok")
        val stray = File(lyricsDir, "3.lrc.tmp")
        assertFalse("temp file must be renamed away on success", stray.exists())
    }

    @Test
    fun write_returnsPathUnderFilesDirLyrics() = runTest {
        val path = store.write(songId = 5L, lrcText = "ok")
        assertTrue(
            "returned path should sit under <filesDir>/lyrics/: $path",
            path.startsWith(lyricsDir.absolutePath + File.separator),
        )
        assertTrue(path.endsWith("5.lrc"))
    }

    @Test
    fun pathFor_isComputableWithoutIO() {
        val path = store.pathFor(123L)
        assertTrue(path.endsWith("lyrics${File.separator}123.lrc"))
    }

    @Test
    fun write_preservesUtf8MultibyteContent() = runTest {
        val utf8 = "[00:01.00]你好\n[00:02.00]привет\n[00:03.00]🎵"
        store.write(songId = 11L, lrcText = utf8)
        assertEquals(utf8, store.read(11L))
    }
}
