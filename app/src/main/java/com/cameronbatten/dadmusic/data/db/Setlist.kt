package com.cameronbatten.dadmusic.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A named ordered collection of songs. In M2 every registered folder becomes
 * exactly one Setlist (name = folder's display name, [sourceFolderUri] = the
 * SAF tree URI). User-authored setlists are a v1.1 scope item but the schema
 * already allows them: `sourceFolderUri` is nullable.
 *
 * `sourceFolderUri` is uniquely indexed so the folder → setlist bridge in
 * [com.cameronbatten.dadmusic.data.scan.LibraryImporter] can upsert on it.
 * Multiple user-authored setlists (both with `sourceFolderUri = null`) are
 * fine — SQLite unique indices allow multiple NULLs.
 */
@Entity(
    tableName = "setlists",
    indices = [Index(value = ["sourceFolderUri"], unique = true)],
)
data class Setlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceFolderUri: String?,
    val orderIndex: Int,
    val createdAt: Long,
    val modifiedUtc: Long,
)
