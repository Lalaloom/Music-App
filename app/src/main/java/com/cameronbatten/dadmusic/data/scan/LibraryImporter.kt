package com.cameronbatten.dadmusic.data.scan

import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Summarises the result of one [LibraryImporter.importAll] run.
 *
 * @param added   Files inserted for the first time (no prior row with this URI).
 * @param updated Files whose URI was already in the DB — the row was replaced (REPLACE strategy).
 * @param skipped Files that were intentionally not imported. Always 0 in Milestone 1;
 *                the field is present to avoid a breaking API change when filtering is added.
 */
data class ImportResult(val added: Int, val updated: Int, val skipped: Int)

private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "aac")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm")

/**
 * Orchestrates the import pipeline: scan → parse → read metadata → upsert into DB.
 *
 * ### FilenameParser injection
 * [FilenameParser] is declared as a Kotlin `object`; Hilt cannot inject singletons that are
 * object declarations.  Rather than adding an artificial `@Provides` wrapper, [LibraryImporter]
 * references [FilenameParser] directly inside [importAll].  The constructor therefore takes only
 * the three injected collaborators: [scanner], [metadataReader], and [songDao].
 *
 * ### Interface extraction
 * [FileScanner] and [MetadataReader] are both final classes with `@ApplicationContext`
 * dependencies that cannot be instantiated in unit tests.  A narrow interface was extracted for
 * each ([FileScannerSource], [MetadataSource]) so that instrumented tests can supply lightweight
 * fakes without real filesystem access or media files.  Production bindings live in
 * [com.cameronbatten.dadmusic.di.ScanBindsModule].
 */
@Singleton
class LibraryImporter @Inject constructor(
    private val scanner: FileScannerSource,
    private val metadataReader: MetadataSource,
    private val songDao: SongDao,
) : LibraryImporterSource {

    /**
     * Scans all registered folders, merges embedded metadata with filename-parsed fallbacks,
     * and upserts every discovered file into the database.
     *
     * Dispatches to [Dispatchers.IO].
     *
     * @return [ImportResult] with counts of added, updated, and skipped files.
     */
    override suspend fun importAll(): ImportResult = withContext(Dispatchers.IO) {
        val files = scanner.scanAll()

        var added = 0
        var updated = 0

        for (file in files) {
            val metadata = metadataReader.read(file.uri)
            val parsed = FilenameParser.parse(file.displayName)

            val title = metadata.title ?: parsed.title ?: file.displayName
            val artist = metadata.artist ?: parsed.artist
            val album = metadata.album
            val durationMs = metadata.durationMs

            val fileType = when (file.extension.lowercase()) {
                in VIDEO_EXTENSIONS -> FileType.VIDEO
                else -> FileType.AUDIO
            }

            val confirmed =
                (metadata.title != null && metadata.artist != null) ||
                (parsed.title != null && parsed.artist != null)

            val uriString = file.uri.toString()
            val existing = songDao.findByUri(uriString)

            val song = Song(
                title = title,
                artist = artist,
                album = album,
                uri = uriString,
                fileType = fileType,
                durationMs = durationMs,
                dateAdded = System.currentTimeMillis(),
                confirmed = confirmed,
            )

            songDao.insert(song)

            if (existing != null) updated++ else added++
        }

        ImportResult(added = added, updated = updated, skipped = 0)
    }
}
