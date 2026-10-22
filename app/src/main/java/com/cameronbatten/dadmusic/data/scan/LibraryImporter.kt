package com.cameronbatten.dadmusic.data.scan

import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.Lyrics
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.LyricsSource
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.lyrics.LrcParser
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
 * Orchestrates the import pipeline: scan → parse → read metadata → upsert → sidecar → prune.
 *
 * ### Rename detection
 * SAF document URIs embed the file's display name on most providers, so renaming
 * a file produces a brand-new URI. The importer loads every existing DB row up
 * front, then — for each scanned file whose URI isn't already in the DB — looks
 * for an orphaned row with the same `sizeBytes` AND `durationMs`. A match is
 * treated as a rename: the existing row's `id` + `dateAdded` are preserved and
 * other fields refresh from disk. Rows left orphaned at the end are pruned.
 *
 * ### Sidecar adoption (spec §5.1)
 * If a `<basename>.lrc` sits next to a music file during scan the scanner attaches
 * its URI to [DiscoveredFile.lrcSidecarUri]. The importer reads the sidecar's
 * content, parses it just to decide `hasTimings`, and upserts a [Lyrics] row with
 * `source = DISK_SIDECAR`. The song's `lyricsStatus` becomes `ADOPTED_FROM_DISK`
 * so the later LRCLIB fetch job (Task 6) skips it. The user's disk `.lrc` is
 * never overwritten — we only read it.
 *
 * ### Why [SongDao.upsert] and not `insert`
 * `insert` is `OnConflict.REPLACE`, which `DELETE + INSERT`s on a row collision.
 * The `lyrics` table has an FK with `ON DELETE CASCADE`, so a plain REPLACE
 * rescan would silently wipe fetched lyrics. [SongDao.upsert] uses INSERT-or-
 * UPDATE semantics that preserve the parent row and therefore its FK children.
 */
@Singleton
class LibraryImporter @Inject constructor(
    private val scanner: FileScannerSource,
    private val metadataReader: MetadataSource,
    private val songDao: SongDao,
    private val lyricsDao: LyricsDao,
    private val sidecarSource: SidecarLyricsSource,
) : LibraryImporterSource {

    override suspend fun importAll(): ImportResult = withContext(Dispatchers.IO) {
        val files = scanner.scanAll()

        val orphans: MutableMap<String, Song> =
            songDao.getAll().associateBy { it.uri }.toMutableMap()

        var added = 0
        var updated = 0

        for (file in files) {
            val uriString = file.uri.toString()
            val metadata = metadataReader.read(file.uri)
            val parsed = com.cameronbatten.dadmusic.data.scan.FilenameParser.parse(file.displayName)

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

            // 1. Same URI → update in place.
            val sameUri = orphans.remove(uriString)
            val songId: Long = if (sameUri != null) {
                val updatedRow = sameUri.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    fileType = fileType,
                    durationMs = durationMs,
                    sizeBytes = file.sizeBytes,
                    confirmed = confirmed,
                    lyricsStatus = statusFor(file, sameUri.lyricsStatus),
                )
                songDao.upsert(updatedRow)
                updated++
                sameUri.id
            } else {
                // 2. Rename twin?
                val renameEntry = orphans.entries.firstOrNull { (_, s) ->
                    s.sizeBytes > 0L &&
                        s.sizeBytes == file.sizeBytes &&
                        s.durationMs == durationMs
                }
                if (renameEntry != null) {
                    orphans.remove(renameEntry.key)
                    val old = renameEntry.value
                    val updatedRow = old.copy(
                        uri = uriString,
                        title = title,
                        artist = artist,
                        album = album,
                        fileType = fileType,
                        durationMs = durationMs,
                        sizeBytes = file.sizeBytes,
                        confirmed = confirmed,
                        lyricsStatus = statusFor(file, old.lyricsStatus),
                    )
                    songDao.upsert(updatedRow)
                    updated++
                    old.id
                } else {
                    // 3. Brand-new file.
                    val newRow = Song(
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uriString,
                        fileType = fileType,
                        durationMs = durationMs,
                        dateAdded = System.currentTimeMillis(),
                        confirmed = confirmed,
                        sizeBytes = file.sizeBytes,
                        lyricsStatus = statusFor(file, LyricsStatus.MISSING),
                    )
                    val insertedId = songDao.upsert(newRow)
                    added++
                    // Upsert returns the generated rowId on insert. For the inserted-only
                    // path this is the new Song.id; look up by uri as a defensive fallback.
                    if (insertedId > 0L) insertedId else (songDao.findByUri(uriString)?.id ?: continue)
                }
            }

            // Adopt sidecar lyrics, if any.
            val sidecarUri = file.lrcSidecarUri
            if (sidecarUri != null) {
                val text = sidecarSource.read(sidecarUri)
                if (text != null) {
                    val hasTimings = LrcParser.parse(text).any { it.isSynced }
                    lyricsDao.upsert(
                        Lyrics(
                            songId = songId,
                            rawLrc = text,
                            hasTimings = hasTimings,
                            source = LyricsSource.DISK_SIDECAR,
                            modifiedUtc = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }

        val removedCount = orphans.size
        for (dbUri in orphans.keys) {
            songDao.deleteByUri(dbUri)
        }

        ImportResult(added = added, updated = updated, removed = removedCount, skipped = 0)
    }

    /**
     * Decide the lyricsStatus to persist for this scan pass.
     *
     * - A sidecar on disk forces [LyricsStatus.ADOPTED_FROM_DISK], even if an older
     *   LRCLIB fetch had succeeded — the user's disk file is authoritative.
     * - Otherwise we preserve whatever status the song already had so a successful
     *   LRCLIB fetch isn't reset to MISSING by every rescan.
     */
    private fun statusFor(file: DiscoveredFile, previous: LyricsStatus): LyricsStatus =
        if (file.lrcSidecarUri != null) LyricsStatus.ADOPTED_FROM_DISK else previous
}
