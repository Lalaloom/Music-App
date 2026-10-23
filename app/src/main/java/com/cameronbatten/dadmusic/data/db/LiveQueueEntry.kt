package com.cameronbatten.dadmusic.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Where the song came from in the context of the current gig:
 * - [SAVED] — copied from a saved [Setlist] when the user opened it.
 * - [REQUEST] — tapped from the right-pane search while a setlist was open.
 *
 * The distinction matters for the "Save tonight's changes to this setlist"
 * button: only mutations relative to the SAVED set count as divergence.
 */
enum class QueueSource { SAVED, REQUEST }

/**
 * One song in the device-local live queue.
 *
 * ### Device-local, never synced
 * This table has intentionally no `modifiedUtc` / `deviceId` columns. Per spec
 * §5.6 the live queue is strictly a performance aid for the current gig and
 * never participates in cloud sync (M4). Save-template-to-setlist is the only
 * path by which live-queue content leaves the device.
 *
 * ### Persistence across process death (Review Focus #3)
 * The queue is kept in Room (not in-memory) so a crash or OS kill during a set
 * doesn't lose the current performance state. On process restart the manager
 * reads this table and resumes the gig.
 *
 * ### `position`
 * Sparse integers (1000, 2000, 3000…), same convention as [SetlistEntry.position].
 */
@Entity(
    tableName = "live_queue",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("songId")],
)
data class LiveQueueEntry(
    @PrimaryKey val position: Int,
    val songId: Long,
    val source: QueueSource,
    val parentSetlistId: Long?,
)
