package com.cameronbatten.dadmusic.data.scan

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [LibraryImporter].
 *
 * Uses:
 * - [FakeFileScanner]   — returns a hardcoded [List<DiscoveredFile>] controlled per-test.
 * - [FakeMetadataReader] — returns a hardcoded [FileMetadata] per URI.
 * - Real in-memory Room DAO — verifies actual upsert/conflict semantics.
 *
 * [FilenameParser] is a Kotlin `object`; [LibraryImporter] references it directly, so no fake
 * is needed here.
 */
@RunWith(AndroidJUnit4::class)
class LibraryImporterTest {

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun discoveredFile(
        uriString: String = "content://test/song.mp3",
        displayName: String = "song.mp3",
        extension: String = "mp3",
    ) = DiscoveredFile(
        uri = Uri.parse(uriString),
        displayName = displayName,
        extension = extension,
        mimeType = null,
        sizeBytes = 1024L,
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
    ): LibraryImporter = LibraryImporter(
        scanner = FakeFileScanner(files),
        metadataReader = FakeMetadataReader(metaByUri),
        songDao = dao,
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * A brand-new file not yet in the DB is counted as `added`.
     */
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
        assertNotNull(dao.findByUri(file.uri.toString()))
    }

    /**
     * Running `importAll` twice on the same URI counts first run as added, second as updated.
     * The DB row must not be duplicated (unique URI index enforces this).
     */
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

        // Only one row should exist for this URI
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
    }

    /**
     * When both metadata title/artist and filename-parsed title/artist are present,
     * the metadata values should be used.
     */
    @Test
    fun importAll_metadataPreferredWhenPresent() = runTest {
        // filename "Title From Name - Artist From Name.mp3" would parse to both title+artist
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

        val song = dao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals("Meta Title", song!!.title)
        assertEquals("Meta Artist", song.artist)
    }

    /**
     * When metadata is absent (all nulls), title and artist fall back to filename parse.
     */
    @Test
    fun importAll_fallsBackToFilenameWhenMetadataMissing() = runTest {
        // "Parsed Title - Parsed Artist.mp3" → FilenameParser yields title="Parsed Title", artist="Parsed Artist"
        val file = discoveredFile(
            "content://test/fallback.mp3",
            "Parsed Title - Parsed Artist.mp3",
            "mp3",
        )
        // Metadata returns all nulls
        val meta = FileMetadata(null, null, null, 0L)
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to meta),
        )

        importer.importAll()

        val song = dao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals("Parsed Title", song!!.title)
        assertEquals("Parsed Artist", song.artist)
    }

    /**
     * A file with extension "mp4" must be stored with [FileType.VIDEO].
     */
    @Test
    fun importAll_mp4File_fileTypeIsVIDEO() = runTest {
        val file = discoveredFile("content://test/video.mp4", "video.mp4", "mp4")
        val meta = fileMetadata(title = "My Video", artist = "Director")
        val importer = buildImporter(
            files = listOf(file),
            metaByUri = mapOf(file.uri.toString() to meta),
        )

        importer.importAll()

        val song = dao.findByUri(file.uri.toString())
        assertNotNull(song)
        assertEquals(FileType.VIDEO, song!!.fileType)
    }
}
