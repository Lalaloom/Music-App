package com.cameronbatten.dadmusic.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PDF document discovered during a library scan. Separate table from
 * [ChordSheet] so filtering / sorting / future features (page bookmarks,
 * annotations) stay cleanly scoped to the format.
 */
@Entity(
    tableName = "pdf_sheets",
    indices = [Index(value = ["uri"], unique = true)],
)
data class PdfSheet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val folderUri: String? = null,
    val folderName: String? = null,
    val dateAdded: Long = 0L,
)
