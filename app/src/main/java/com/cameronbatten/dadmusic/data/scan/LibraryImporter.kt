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
 * File renames are counted inside [updated] (same row, new URI) — not as `added + removed`.
 *
 * @param added   Files inserted for the first time (no prior row with this URI or matching size+duration).
 * @param updated Rows re-upserted for an existing URI, OR rows whose URI changed because the user
 *                renamed the underlying file (detected via matching size + duration).
 * @param removed Rows deleted because the underlying file is no longer present and no rename match was found.
 * @param skipped Files intentionally not imported. Always 0 in Milestone 1; present to avoid a
 *                breaking API change when filtering is added.
 */
data class ImportResult(
    val added: Int,
    val updated: Int,
    val removed: Int = 0,
    val skipped: Int = 0,
)

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm")

/**
 * Orchestrates the import pipeline: scan → parse → read metadata → upsert → prune.
 *
 * ### Rename detection
 * SAF document URIs embed the file's display name on most providers, so renaming
 * a file in a file manager produces a brand-new URI. If we only matched on URI,
 * a rename would show up as `added + removed`, duplicating the song in the UI
 * for the user who just wanted to retitle it.
 *
 * To avoid that, the importer loads every existing DB row up front, then — for
 * each scanned file whose URI isn't already in the DB — looks for an orphaned
 * row with the same `sizeBytes` AND `durationMs`. A match is treated as a rename:
 * the existing row's `id` and `dateAdded` are preserved, and its `uri`/`title`/
 * other fields are refreshed from the new file. Rows left orphaned at the end
 * are genuinely gone from disk and are deleted.
 *
 * Collisions require two files with identical byte count and identical sub-second
 * duration — vanishingly unlikely in a personal music library, acceptable for M1.
 */
@Singleton
class LibraryImporter @Inject constructor(
    private val scanner: FileScannerSource,
    private val metadataReader: MetadataSource,
    private val songDao: SongDao,
) : LibraryImporterSource {

    override suspend fun importAll(): ImportResult = withContext(Dispatchers.IO) {
        val files = scanner.scanAll()

        // All current rows keyed by URI; we remove entries as we match them so
        // what's left at the end is what the user removed from disk.
        val orphans: MutableMap<String, Song> =
            songDao.getAll().associateBy { it.uri }.toMutableMap()

        var added = 0
        var updated = 0

        for (file in files) {
            val uriString = file.uri.toString()
            val metadata = metadataReader.read(file.uri)
            val parsed = FilenameParser.parse(file.displayName)

            val title = metadata.title ?: parsed.title
                ?: file.displayName.substringBeforeLast('.', file.displayName)
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

            // 1. Same URI → update in place (preserve id + dateAdded).
            val sameUri = orphans.remove(uriString)
            if (sameUri != null) {
                songDao.insert(
                    sameUri.copy(
                        title = title,
                        artist = artist,
                        album = album,
                        fileType = fileType,
                        durationMs = durationMs,
                        sizeBytes = file.sizeBytes,
                        confirmed = confirmed,
                    ),
                )
                updated++
                continue
            }

            // 2. No URI match — look for a rename twin (same size + duration).
            //    Guard sizeBytes > 0 to avoid collapsing multiple truly-unknown entries.
            val renameEntry = orphans.entries.firstOrNull { (_, s) ->
                s.sizeBytes > 0L &&
                    s.sizeBytes == file.sizeBytes &&
                    s.durationMs == durationMs
            }
            if (renameEntry != null) {
                orphans.remove(renameEntry.key)
                songDao.insert(
                    renameEntry.value.copy(
                        uri = uriString,
                        title = title,
                        artist = artist,
                        album = album,
                        fileType = fileType,
                        durationMs = durationMs,
                        sizeBytes = file.sizeBytes,
                        confirmed = confirmed,
                    ),
                )
                updated++
                continue
            }

            // 3. Genuinely new file.
            songDao.insert(
                Song(
                    title = title,
                    artist = artist,
                    album = album,
                    uri = uriString,
                    fileType = fileType,
                    durationMs = durationMs,
                    dateAdded = System.currentTimeMillis(),
                    confirmed = confirmed,
                    sizeBytes = file.sizeBytes,
                ),
            )
            added++
        }

        // 4. Prune: whatever's left is gone from disk.
        val removedCount = orphans.size
        for (dbUri in orphans.keys) {
            songDao.deleteByUri(dbUri)
        }

        ImportResult(added = added, updated = updated, removed = removedCount, skipped = 0)
    }
}
