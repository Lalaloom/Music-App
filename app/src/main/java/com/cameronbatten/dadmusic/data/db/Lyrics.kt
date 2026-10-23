package com.cameronbatten.dadmusic.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Where a [Lyrics] row's text came from. Lets us honour pre-existing disk sidecars
 * (never overwritten) and distinguish user-edited corrections from fetched content.
 */
enum class LyricsSource { DISK_SIDECAR, LRCLIB, USER_EDIT }

/**
 * Lyrics attached to a [Song]. Unique per song; replacing the row for a given
 * [songId] (REPLACE conflict strategy on [LyricsDao.upsert]) is how we handle
 * refetches and user edits.
 *
 * @param rawLrc     Full LRC file text, verbatim. Preserved for user inspection
 *                   even when parsing fails.
 * @param hasTimings True when the parser found at least one `[mm:ss.xx]` tag.
 *                   Drives whether the Zen view auto-scrolls or distributes
 *                   lines linearly (spec §5.2).
 * @param source     Origin of the content; see [LyricsSource].
 * @param modifiedUtc Unix millis of last write — used by future sync (M4+) to
 *                    resolve conflicts last-write-wins.
 */
@Entity(
    tableName = "lyrics",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["songId"], unique = true)],
)
data class Lyrics(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val rawLrc: String,
    val hasTimings: Boolean,
    val source: LyricsSource,
    val modifiedUtc: Long,
)
