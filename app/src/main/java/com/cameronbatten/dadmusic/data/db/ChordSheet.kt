package com.cameronbatten.dadmusic.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A chord-sheet file (`.cho` / `.chordpro` / `.pro` / `.crd`) discovered during
 * a library scan. Chord sheets are *documents*, not playable media — they live
 * in their own table so the `songs` table and `FileType` enum stay a strict
 * mp3/mp4 model.
 *
 * @param uri         SAF document URI pointing at the chord-sheet file.
 * @param title       Display title. First `{title: ...}` directive from the
 *                    file content if present, else the filename without
 *                    extension.
 * @param artist      First `{artist: ...}` or `{subtitle: ...}` directive if
 *                    present. Nullable.
 * @param folderUri   SAF root-folder URI the file was discovered under (used
 *                    for pruning when a folder is removed).
 * @param folderName  Display name of the root folder at scan time.
 * @param dateAdded   Epoch ms of first discovery. Preserved on rescan.
 */
@Entity(
    tableName = "chord_sheets",
    indices = [Index(value = ["uri"], unique = true)],
)
data class ChordSheet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val artist: String? = null,
    val folderUri: String? = null,
    val folderName: String? = null,
    val dateAdded: Long = 0L,
)
