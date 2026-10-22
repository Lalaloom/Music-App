package com.cameronbatten.dadmusic.data.scan

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.LyricsSource
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [LibraryImporter].
 *
 * Uses:
 * - [FakeFileScanner]    — returns a hardcoded [List<DiscoveredFile>] controlled per-test.
 * - [FakeMetadataReader] — returns a hardcoded [FileMetadata] per URI.
 * - [FakeSidecarSource]  — returns a hardcoded sidecar text per URI.
 * - Real in-memory Room — verifies actual upsert / FK / cascade semantics.
 */
@RunWith(AndroidJUnit4::class)
class LibraryImporterTest {

    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao
    private lateinit var lyricsDao: LyricsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        songDao = db.songDao()
        lyricsDao = db.lyricsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeFileScanner(private val files: List<DiscoveredFile>) : FileScannerSource {
        override suspend fun scanAll(): List<DiscoveredFile> = files
    }

    private class FakeMetadataReader(
        private val byUri: Map<String, FileMetadata>,
    ) : MetadataSource {
        override suspend fun read(uri: Uri): FileMetadata =
            byUri[uri.toString()] ?: FileMetadata(null, null, null, 0L)
    }

    private class FakeSidecarSource(
        private val byUri: Map<String, String?> = emptyMap(),
    ) : SidecarLyricsSource {
        override suspend fun read(uri: Uri): String? = byUri[uri.toString()]
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun discoveredFile(
        uriString: String = "content://test/song.mp3",
        displayName: String = "song.mp3",
        extension: String = "mp3",
        lrcSidecarUri: Uri? = null,
    ) = DiscoveredFile(
        uri = Uri.parse(uriString),
        displayName = displayName,
        extension = extension,
        mimeType = null,
        sizeBytes = 1024L,
        lrcSidecarUri = lrcSidecarUri,
    )

    private fun fileMetadata(
        title: String? = "Meta Title",
        artist: String? = "Meta Artist",
        album: String? = "Meta Album",
        durationMs: Long = 180_000L,
    ) = FileMetadata(title, artist, album, durationMs)

    private fun buildImporter(
        files: List<DiscoveredFile>,
        metaByUri: Map<String, FileMetadata>,
        sidecarByUri: Map<String, String?> = emptyMap(),
    ): LibraryImporter = LibraryImporter(
        scanner = FakeFileScanner(files),
        metadataReader = FakeMetadataReader(metaByUri),
        songDao = songDao,
        lyricsDao = lyricsDao,
        sidecarSource = FakeSidecarSource(sidecarByUri),
    )

    // ── Core upsert tests (from M1, still valid) ──────────────────────────────

    @Test
    fun importAll_newFile_insertsAsAdded() = runTest {
        val file = discoveredFile("content://test/new.mp3", "new.mp3", "mp3")
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to fileMetadata()),
        )

        val result = importer.importAll()

        assertEquals(1, result.added)
        assertEquals(0, result.updated)
        assertEquals(0, result.skipped)
        assertNotNull(songDao.findByUri(file.uri.toString()))
    }

    @Test
    fun importAll_sameUriTwice_countsAsUpdatedNotDuplicated() = runTest {
        val file = discoveredFile("content://test/dup.mp3", "dup.mp3", "mp3")
        val meta = fileMetadata()
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to meta),
        )

        val first = importer.importAll()
        val second = importer.importAll()

        assertEquals(1, first.added)
        assertEquals(1, second.updated)
        assertEquals(0, second.added)

        val all = songDao.observeAll().first()
        assertEquals(1, all.size)
    }

    @Test
    fun importAll_metadataPreferredWhenPresent() = runTest {
        val file = discoveredFile(
            "content://test/prefer_meta.mp3",
            "Title From Name - Artist From Name.mp3",
            "mp3",
        )
        val meta = fileMetadata(title = "Meta Title", artist = "Meta Artist")
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to meta),
        )

        importer.importAll()

        val song = songDao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals("Meta Title", song!!.title)
        assertEquals("Meta Artist", song.artist)
    }

    @Test
    fun importAll_fallsBackToFilenameWhenMetadataMissing() = runTest {
        val file = discoveredFile(
            "content://test/fallback.mp3",
            "Parsed Title - Parsed Artist.mp3",
            "mp3",
        )
        val meta = FileMetadata(null, null, null, 0L)
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to meta),
        )

        importer.importAll()

        val song = songDao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals("Parsed Title", song!!.title)
        assertEquals("Parsed Artist", song.artist)
    }

    @Test
    fun importAll_mp4File_fileTypeIsVIDEO() = runTest {
        val file = discoveredFile("content://test/video.mp4", "video.mp4", "mp4")
        val meta = fileMetadata(title = "My Video", artist = "Director")
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to meta),
        )

        importer.importAll()

        val song = songDao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals(FileType.VIDEO, song!!.fileType)
    }

    // ── Sidecar adoption (Review Focus #2) ────────────────────────────────────

    @Test
    fun importAll_adoptsSidecarWhenPresent() = runTest {
        val sidecarUri = Uri.parse("content://test/song.lrc")
        val file = discoveredFile(
            uriString = "content://test/song.mp3",
            lrcSidecarUri = sidecarUri,
        )
        val lrc = "[00:10.00]Hello\n[00:12.00]World"
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to fileMetadata()),
            sidecarByUri = mapOf(sidecarUri.toString() to lrc),
        )

        importer.importAll()

        val song = songDao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals(LyricsStatus.ADOPTED_FROM_DISK, song!!.lyricsStatus)

        val lyrics = lyricsDao.findBySongId(song.id)
        assertNotNull(lyrics)
        assertEquals(lrc, lyrics!!.rawLrc)
        assertEquals(LyricsSource.DISK_SIDECAR, lyrics.source)
        assertTrue("Synced LRC must set hasTimings = true", lyrics.hasTimings)
    }

    @Test
    fun importAll_unsyncedSidecarMarksHasTimingsFalse() = runTest {
        val sidecarUri = Uri.parse("content://test/plain.lrc")
        val file = discoveredFile(
            uriString = "content://test/plain.mp3",
            lrcSidecarUri = sidecarUri,
        )
        val plainText = "verse one line one\nverse one line two"
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to fileMetadata()),
            sidecarByUri = mapOf(sidecarUri.toString() to plainText),
        )

        importer.importAll()

        val song = songDao.findByUri(file.uri.toString())
        val lyrics = lyricsDao.findBySongId(song!!.id)
        assertNotNull(lyrics)
        assertFalse("Plain-text sidecar must set hasTimings = false", lyrics!!.hasTimings)
    }

    @Test
    fun importAll_sidecarWithUnreadableContentSkipsLyricsRow() = runTest {
        // sidecarSource.read() returning null simulates a corrupt / unreadable sidecar.
        val sidecarUri = Uri.parse("content://test/bad.lrc")
        val file = discoveredFile(
            uriString = "content://test/bad.mp3",
            lrcSidecarUri = sidecarUri,
        )
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to fileMetadata()),
            sidecarByUri = mapOf(sidecarUri.toString() to null),
        )

        importer.importAll()

        val song = songDao.findByUri(file.uri.toString())
        assertNotNull(song)
        // Status still reflects the attempt — downstream fetch is skipped either way,
        // and the user's "something is wrong with this file" lives in the lyrics row's absence.
        assertEquals(LyricsStatus.ADOPTED_FROM_DISK, song!!.lyricsStatus)
        assertNull(lyricsDao.findBySongId(song.id))
    }

    @Test
    fun rescan_withoutSidecar_preservesPreviouslyFetchedLyrics() = runTest {
        // Simulates: user ran a LRCLIB fetch (not yet implemented, but we persist the row
        // directly here), then triggered a rescan. The rescan must NOT wipe the lyrics.
        val file = discoveredFile(
            uriString = "content://test/preserve.mp3",
            lrcSidecarUri = null,
        )
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to fileMetadata()),
        )

        importer.importAll()
        val song = songDao.findByUri(file.uri.toString())!!

        // Simulate a successful fetch landing a Lyrics row.
        lyricsDao.upsert(
            com.cameronbatten.dadmusic.data.db.Lyrics(
                songId = song.id,
                rawLrc = "[00:00.00]from LRCLIB",
                hasTimings = true,
                source = LyricsSource.LRCLIB,
                modifiedUtc = 1L,
            ),
        )

        // Rescan — must not cascade-delete the Lyrics row.
        importer.importAll()

        val stillThere = lyricsDao.findBySongId(song.id)
        assertNotNull("Rescan must preserve existing LRCLIB-fetched lyrics", stillThere)
        assertEquals(LyricsSource.LRCLIB, stillThere!!.source)
    }
}
