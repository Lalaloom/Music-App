package com.cameronbatten.dadmusic.data.db

import androidx.room.Embedded

/**
 * Projection returned by [SetlistDao.entriesFor] / [SetlistDao.observeEntries].
 * Carries the full [Song] plus the entry's `position` so the UI can render the
 * ordered setlist without a second query.
 */
data class SongWithPosition(
    @Embedded val song: Song,
    val position: Int,
)
