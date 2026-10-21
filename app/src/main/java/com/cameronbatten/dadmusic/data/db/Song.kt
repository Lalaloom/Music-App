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
)
