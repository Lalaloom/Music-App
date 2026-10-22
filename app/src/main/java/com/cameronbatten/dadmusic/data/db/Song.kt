package com.cameronbatten.dadmusic.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FileType { AUDIO, VIDEO }

@Entity(
    tableName = "songs",
    indices = [Index(value = ["uri"], unique = true)],
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String?,
    val album: String?,
    @ColumnInfo(name = "uri") val uri: String,
    val fileType: FileType,
    val durationMs: Long,
    val dateAdded: Long,
    val confirmed: Boolean,
    // sizeBytes is used to detect file renames: when a scan finds a URI we
    // haven't seen before, we check whether an orphaned row has the same size +
    // duration and treat that as the same file under a new name.
    val sizeBytes: Long = 0L,
    val lyricsStatus: LyricsStatus = LyricsStatus.MISSING,
    val lyricsCachePath: String? = null,
)
