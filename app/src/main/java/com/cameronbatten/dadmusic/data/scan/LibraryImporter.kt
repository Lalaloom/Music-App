package com.cameronbatten.dadmusic.data.scan

import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.data.db.Lyrics
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.LyricsSource
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.Setlist
import com.cameronbatten.dadmusic.data.db.SetlistDao
import com.cameronbatten.dadmusic.data.db.SetlistEntry
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
 * @param added   Files inserted for the first time.
 * @param updated Rows re-upserted for an existing URI or where a rename was detected
 *                via matching size + duration.
 * @param removed Rows deleted because the underlying file is no longer present and no
 *                rename match was found.
 * @param skipped Files intentionally not imported. Always 0 in Milestone 1; present
 *                to avoid a breaking API change when filtering is added.
 */
data class ImportResult(
    val added: Int,
    val updated: Int,
    val removed: Int = 0,
    val skipped: Int = 0,
)

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm")
private val CHORD_EXTENSIONS = setOf("cho", "chordpro", "pro", "crd")
private val PDF_EXTENSIONS = setOf("pdf")
private const val SETLIST_POSITION_STEP = 1000

/**
 * Orchestrates the import pipeline: scan → parse → read metadata → upsert songs →
 * adopt sidecar lyrics → sync setlists per folder → prune removed songs/setlists.
 *
 * ### Rename detection
 * SAF document URIs embed display names on most providers, so renaming a file
 * produces a new URI. The importer loads every DB row up front; for a scanned
 * file whose URI isn't in the DB it looks for an orphan with matching
 * (sizeBytes, durationMs) and treats a hit as a rename — preserves `id` and
 * `dateAdded`, refreshes other columns.
 *
 * ### Folder → setlist bridge (Task 8)
 * Every file carries its root `rootFolderUri` + `rootFolderName`. After the
 * song-upsert pass the importer groups scanned songs by their root folder,
 * upserts a Setlist for each (keyed by `sourceFolderUri`), and replaces that
 * setlist's entries with the current song set in scan order. Setlists for
 * folders no longer present in the scan output are deleted — this is how
 * unregistering a folder makes its setlist disappear.
 *
 * ### Why [SongDao.upsert] not `insert`
 * `insert` is `OnConflict.REPLACE`, which `DELETE + INSERT`s on a row collision.
 * Lyrics/SetlistEntry FKs have `ON DELETE CASCADE`, so plain REPLACE rescans
 * would silently wipe fetched lyrics and setlist membership. [SongDao.upsert]
 * uses INSERT-or-UPDATE semantics that preserve parent row identity.
 */
@Singleton
class LibraryImporter @Inject constructor(
    private val scanner: FileScannerSource,
    private val metadataReader: MetadataSource,
    private val songDao: SongDao,
    private val lyricsDao: LyricsDao,
    private val sidecarSource: SidecarLyricsSource,
    private val setlistDao: SetlistDao,
    private val chordSheetDao: com.cameronbatten.dadmusic.data.db.ChordSheetDao,
    private val pdfSheetDao: com.cameronbatten.dadmusic.data.db.PdfSheetDao,
) : LibraryImporterSource {

    override suspend fun importAll(): ImportResult = withContext(Dispatchers.IO) {
        val files = scanner.scanAll()

        val orphans: MutableMap<String, Song> =
            songDao.getAll().associateBy { it.uri }.toMutableMap()
        val chordOrphans: MutableMap<String, com.cameronbatten.dadmusic.data.db.ChordSheet> =
            chordSheetDao.getAll().associateBy { it.uri }.toMutableMap()
        val pdfOrphans: MutableMap<String, com.cameronbatten.dadmusic.data.db.PdfSheet> =
            pdfSheetDao.getAll().associateBy { it.uri }.toMutableMap()

        var added = 0
        var updated = 0

        // Collect (folderUri → ordered songIds) as we upsert so we can rebuild
        // setlist entries in one pass after the song loop.
        val songIdByFolder: MutableMap<String, MutableList<Long>> = linkedMapOf()
        val folderNames: MutableMap<String, String> = linkedMapOf()

        for (file in files) {
            val uriString = file.uri.toString()
            val ext = file.extension.lowercase()

            // ChordPro files land in their own table — not songs. (M5)
            if (ext in CHORD_EXTENSIONS) {
                processChordSheet(file, uriString, chordOrphans)
                continue
            }
            // PDF sheet music lands in its own table. (M8)
            if (ext in PDF_EXTENSIONS) {
                processPdfSheet(file, uriString, pdfOrphans)
                continue
            }

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

            val sameUri = orphans.remove(uriString)
            val songId: Long = if (sameUri != null) {
                songDao.upsert(
                    sameUri.copy(
                        title = title,
                        artist = artist,
                        album = album,
                        fileType = fileType,
                        durationMs = durationMs,
                        sizeBytes = file.sizeBytes,
                        confirmed = confirmed,
                        lyricsStatus = statusFor(file, sameUri.lyricsStatus),
                    ),
                )
                updated++
                sameUri.id
            } else {
                val renameEntry = orphans.entries.firstOrNull { (_, s) ->
                    s.sizeBytes > 0L && s.sizeBytes == file.sizeBytes && s.durationMs == durationMs
                }
                if (renameEntry != null) {
                    orphans.remove(renameEntry.key)
                    val old = renameEntry.value
                    songDao.upsert(
                        old.copy(
                            uri = uriString,
                            title = title,
                            artist = artist,
                            album = album,
                            fileType = fileType,
                            durationMs = durationMs,
                            sizeBytes = file.sizeBytes,
                            confirmed = confirmed,
                            lyricsStatus = statusFor(file, old.lyricsStatus),
                        ),
                    )
                    updated++
                    old.id
                } else {
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
                    if (insertedId > 0L) insertedId
                    else (songDao.findByUri(uriString)?.id ?: continue)
                }
            }

            // Track folder → songs for the setlist pass.
            val folderUri = file.rootFolderUri
            if (folderUri != null) {
                songIdByFolder.getOrPut(folderUri) { mutableListOf() }.add(songId)
                file.rootFolderName?.let { folderNames[folderUri] = it }
            }

            // Adopt sidecar lyrics if any — but never overwrite a USER_EDIT row.
            val sidecarUri = file.lrcSidecarUri
            if (sidecarUri != null) {
                val existingLyrics = lyricsDao.findBySongId(songId)
                if (existingLyrics?.source == LyricsSource.USER_EDIT) {
                    // Review Focus #5: user-authored content wins. Leave the row alone.
                } else {
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
        }

        // Prune songs whose files are gone.
        val removedCount = orphans.size
        for (dbUri in orphans.keys) {
            songDao.deleteByUri(dbUri)
        }
        // Prune chord sheets whose files are gone.
        for (dbUri in chordOrphans.keys) {
            chordSheetDao.deleteByUri(dbUri)
        }
        // Prune PDFs whose files are gone.
        for (dbUri in pdfOrphans.keys) {
            pdfSheetDao.deleteByUri(dbUri)
        }

        syncSetlists(songIdByFolder, folderNames)

        ImportResult(added = added, updated = updated, removed = removedCount, skipped = 0)
    }

    /**
     * Upsert a chord-sheet row for a `.cho` / `.chordpro` / `.pro` / `.crd`
     * file. Pulls title + artist from the file's own ChordPro directives when
     * present; falls back to the filename (minus extension) for the title.
     * Reuses [sidecarSource] as a generic SAF-document text reader.
     */
    /** PDF files: minimal row keyed by URI; filename sans extension is the title. */
    private suspend fun processPdfSheet(
        file: DiscoveredFile,
        uriString: String,
        pdfOrphans: MutableMap<String, com.cameronbatten.dadmusic.data.db.PdfSheet>,
    ) {
        val title = file.displayName.substringBeforeLast('.', file.displayName)
        val existing = pdfOrphans.remove(uriString)
        val row = (existing ?: com.cameronbatten.dadmusic.data.db.PdfSheet(
            uri = uriString,
            title = title,
            folderUri = file.rootFolderUri,
            folderName = file.rootFolderName,
            dateAdded = System.currentTimeMillis(),
        )).copy(
            title = title,
            folderUri = file.rootFolderUri,
            folderName = file.rootFolderName,
        )
        pdfSheetDao.upsert(row)
    }

    private suspend fun processChordSheet(
        file: DiscoveredFile,
        uriString: String,
        chordOrphans: MutableMap<String, com.cameronbatten.dadmusic.data.db.ChordSheet>,
    ) {
        val text = sidecarSource.read(file.uri)
        val parsed = text?.let { com.cameronbatten.dadmusic.chord.ChordProParser.parse(it) }
        val title = parsed?.title
            ?: file.displayName.substringBeforeLast('.', file.displayName)
        val artist = parsed?.artist

        val existing = chordOrphans.remove(uriString)
        val row = (existing ?: com.cameronbatten.dadmusic.data.db.ChordSheet(
            uri = uriString,
            title = title,
            artist = artist,
            folderUri = file.rootFolderUri,
            folderName = file.rootFolderName,
            dateAdded = System.currentTimeMillis(),
        )).copy(
            title = title,
            artist = artist,
            folderUri = file.rootFolderUri,
            folderName = file.rootFolderName,
        )
        chordSheetDao.upsert(row)
    }

    /**
     * For each scanned folder, upsert a Setlist and replace its entries. Delete
     * any pre-existing Setlist whose `sourceFolderUri` wasn't in this scan.
     */
    private suspend fun syncSetlists(
        songIdByFolder: Map<String, List<Long>>,
        folderNames: Map<String, String>,
    ) {
        val now = System.currentTimeMillis()
        val existingByFolder = setlistDao.getAll()
            .filter { it.sourceFolderUri != null }
            .associateBy { it.sourceFolderUri!! }

        val scannedFolders = songIdByFolder.keys

        for ((folderUri, songIds) in songIdByFolder) {
            val existing = existingByFolder[folderUri]
            val toUpsert = Setlist(
                id = existing?.id ?: 0,
                name = folderNames[folderUri] ?: existing?.name ?: "Unnamed",
                sourceFolderUri = folderUri,
                orderIndex = existing?.orderIndex ?: 0,
                createdAt = existing?.createdAt ?: now,
                modifiedUtc = now,
            )
            val resolvedId = setlistDao.upsert(toUpsert).takeIf { it > 0L }
                ?: existing?.id
                ?: setlistDao.findByFolder(folderUri)?.id
                ?: continue

            val entries = songIds.distinct().mapIndexed { i, songId ->
                SetlistEntry(
                    setlistId = resolvedId,
                    songId = songId,
                    position = (i + 1) * SETLIST_POSITION_STEP,
                )
            }
            setlistDao.replaceEntries(resolvedId, entries)
        }

        // Prune: folder-sourced setlists whose folder wasn't scanned.
        for ((folderUri, setlist) in existingByFolder) {
            if (folderUri !in scannedFolders) {
                setlistDao.deleteById(setlist.id)
            }
        }
    }

    private fun statusFor(file: DiscoveredFile, previous: LyricsStatus): LyricsStatus =
        if (file.lrcSidecarUri != null) LyricsStatus.ADOPTED_FROM_DISK else previous
}
