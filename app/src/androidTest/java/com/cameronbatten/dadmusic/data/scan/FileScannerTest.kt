package com.cameronbatten.dadmusic.data.scan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.folder.FolderUriSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for [FileScanner].
 *
 * Fixtures use [DocumentFile.fromFile] backed by [Context.filesDir] scratch files so that the
 * real DocumentFile traversal path is exercised without requiring a connected device with SAF
 * permissions.  [FolderUriSource] is supplied by a plain in-test fake — no mocking framework
 * required.
 *
 * NOTE: Compile check only — full execution requires an emulator/device (Task 12 smoke).
 */
@RunWith(AndroidJUnit4::class)
class FileScannerTest {

    private lateinit var context: Context
    private lateinit var scratchDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Use a fresh scratch directory under filesDir so each test starts clean.
        scratchDir = context.filesDir.resolve("scan_test_scratch").also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates an empty file at [path] relative to [scratchDir], creating parent dirs as needed. */
    private fun touch(path: String): File =
        scratchDir.resolve(path).also { it.parentFile?.mkdirs(); it.createNewFile() }

    /**
     * Minimal [FolderUriSource] fake that returns [uris] from [FolderUriSource.resolvableUris].
     * No [Context] dependency — the interface is the only test seam needed.
     */
    private fun sourceOf(vararg uris: Uri): FolderUriSource {
        val uriList: List<Uri> = uris.toList()
        return object : FolderUriSource {
            override fun resolvableUris(): List<Uri> = uriList
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Given two registered folders each containing one music file, [FileScanner.scanAll]
     * must return both files.
     */
    @Test
    fun scanAll_returnsFilesFromAllRegisteredFolders() = runTest {
        val folderA = scratchDir.resolve("folderA").also { it.mkdirs() }
        val folderB = scratchDir.resolve("folderB").also { it.mkdirs() }
        folderA.resolve("song_a.mp3").createNewFile()
        folderB.resolve("song_b.flac").createNewFile()

        val scanner = FileScanner(
            context,
            sourceOf(
                Uri.fromFile(folderA),
                Uri.fromFile(folderB),
            ),
        )

        val results = scanner.scanAll()

        assertEquals("Expected 2 files from 2 folders", 2, results.size)
        val names = results.map { it.displayName }.toSet()
        assertTrue(names.contains("song_a.mp3"))
        assertTrue(names.contains("song_b.flac"))
    }

    /**
     * Files with non-music extensions (e.g. .txt, .jpg) must be excluded from results.
     */
    @Test
    fun scanAll_skipsNonMusicExtensions() = runTest {
        val folder = scratchDir.resolve("mixed").also { it.mkdirs() }
        folder.resolve("track.mp3").createNewFile()
        folder.resolve("photo.jpg").createNewFile()
        folder.resolve("notes.txt").createNewFile()
        folder.resolve("video.mp4").createNewFile()

        val scanner = FileScanner(context, sourceOf(Uri.fromFile(folder)))

        val results = scanner.scanAll()

        assertEquals("Only mp3 and mp4 should pass the filter", 2, results.size)
        val extensions = results.map { it.extension }.toSet()
        assertTrue(extensions.containsAll(setOf("mp3", "mp4")))
    }

    /**
     * [FileScanner.scanAll] must recurse into subdirectories and surface files found there.
     */
    @Test
    fun scanAll_includesFilesInSubdirectories() = runTest {
        val root = scratchDir.resolve("root").also { it.mkdirs() }
        touch("root/top.mp3")
        touch("root/sub/deep.m4a")
        touch("root/sub/sub2/deeper.wav")

        val scanner = FileScanner(context, sourceOf(Uri.fromFile(root)))

        val results = scanner.scanAll()

        assertEquals("Expected 3 files across root and subdirectories", 3, results.size)
        val names = results.map { it.displayName }.toSet()
        assertTrue(names.contains("top.mp3"))
        assertTrue(names.contains("deep.m4a"))
        assertTrue(names.contains("deeper.wav"))
    }

    /**
     * If the same URI appears via multiple registered folders (or the same file is reachable by
     * two different registered roots), [FileScanner.scanAll] must deduplicate and return it once.
     */
    @Test
    fun scanAll_dedupesIfSameUriAcrossRegisteredFolders() = runTest {
        val folder = scratchDir.resolve("dedup").also { it.mkdirs() }
        folder.resolve("track.mp3").createNewFile()

        // Register the same folder URI twice — the scanner must not return duplicates.
        val folderUri = Uri.fromFile(folder)
        val scanner = FileScanner(context, sourceOf(folderUri, folderUri))

        val results = scanner.scanAll()

        assertEquals("Duplicate URI must be deduped to a single result", 1, results.size)
    }
}
