package com.cameronbatten.dadmusic.setlist

import com.cameronbatten.dadmusic.data.db.LiveQueueDao
import com.cameronbatten.dadmusic.data.db.LiveQueueEntry
import com.cameronbatten.dadmusic.data.db.QueueSource
import com.cameronbatten.dadmusic.data.db.SetlistDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device-local live queue facade over [LiveQueueDao] + [SetlistDao].
 *
 * All queue state lives in Room (see [LiveQueueEntry] KDoc) so a process-death
 * mid-gig doesn't lose the current performance order (spec §5.6, Review Focus #3).
 * The manager itself holds no mutable in-memory state — "which setlist is this
 * queue a copy of" is derived from any entry's `parentSetlistId`.
 *
 * Positions are kept sparse (1000, 2000, 3000…) so single-row inserts have room
 * to take midpoints; whole-table rewrites (open, move) renumber from scratch.
 */
@Singleton
class LiveQueueManager @Inject constructor(
    private val liveQueueDao: LiveQueueDao,
    private val setlistDao: SetlistDao,
) {

    fun observe(): Flow<List<LiveQueueEntry>> = liveQueueDao.observeAll()

    suspend fun snapshot(): List<LiveQueueEntry> = liveQueueDao.getAll()

    /**
     * Replace the entire queue with a copy of [setlistId]'s saved entries.
     * Every entry is tagged [QueueSource.SAVED] and keeps `parentSetlistId = setlistId`
     * so later divergence from the saved template can be detected.
     */
    suspend fun openSetlist(setlistId: Long) {
        val saved = setlistDao.entriesFor(setlistId)
        val rebuilt = saved.mapIndexed { i, swp ->
            LiveQueueEntry(
                position = (i + 1) * POSITION_STEP,
                songId = swp.song.id,
                source = QueueSource.SAVED,
                parentSetlistId = setlistId,
            )
        }
        liveQueueDao.replaceAll(rebuilt)
    }

    /** Clears the queue entirely — e.g., when the user switches to the Library virtual view. */
    suspend fun clear() {
        liveQueueDao.deleteAll()
    }

    /** Append a song to the end of the queue. Returns the row that was inserted. */
    suspend fun append(songId: Long, source: QueueSource): LiveQueueEntry {
        val maxPos = liveQueueDao.maxPosition() ?: 0
        val parent = liveQueueDao.getAll().firstOrNull()?.parentSetlistId
        val entry = LiveQueueEntry(
            position = maxPos + POSITION_STEP,
            songId = songId,
            source = source,
            parentSetlistId = parent,
        )
        liveQueueDao.insert(entry)
        return entry
    }

    suspend fun remove(position: Int) {
        liveQueueDao.deleteByPosition(position)
    }

    /**
     * Rewrite the queue in the given [songIds] order. Each existing entry's
     * `source` and `parentSetlistId` are preserved; positions are renumbered
     * from scratch using the sparse step. Song IDs not currently in the queue
     * are dropped silently (defensive — the UI should pass a permutation).
     *
     * Preferred over [moveByIndex] when the caller has a final desired order
     * — one DB write, no per-move chatter.
     */
    suspend fun applyOrder(songIds: List<Long>) {
        val existing = liveQueueDao.getAll().associateBy { it.songId }
        val reordered = songIds.mapIndexedNotNull { i, id ->
            existing[id]?.copy(position = (i + 1) * POSITION_STEP)
        }
        liveQueueDao.replaceAll(reordered)
    }

    /**
     * Move the entry currently at [fromIndex] to [toIndex] in the queue's visual
     * order. Both indices are 0-based list indices as rendered by the UI; the
     * resulting rows are renumbered with fresh sparse positions.
     *
     * No-op when either index is out of range or when `fromIndex == toIndex`.
     */
    suspend fun moveByIndex(fromIndex: Int, toIndex: Int) {
        val current = liveQueueDao.getAll().toMutableList()
        if (fromIndex !in current.indices) return
        if (toIndex !in current.indices) return
        if (fromIndex == toIndex) return

        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)

        val renumbered = current.mapIndexed { i, e ->
            e.copy(position = (i + 1) * POSITION_STEP)
        }
        liveQueueDao.replaceAll(renumbered)
    }

    /** Returns the setlist this queue is a copy of, or null when the user is in Library view. */
    suspend fun currentParentSetlistId(): Long? =
        liveQueueDao.getAll().firstOrNull()?.parentSetlistId

    /**
     * True if the current queue's order or membership differs from the saved
     * setlist it was opened from. Returns false when the queue is empty, when
     * the parent setlist no longer exists, or when the queue is attached to no
     * parent (Library view).
     */
    suspend fun hasDivergedFromParent(): Boolean {
        val current = liveQueueDao.getAll()
        val parentId = current.firstOrNull()?.parentSetlistId ?: return false
        val saved = setlistDao.entriesFor(parentId)
        if (current.size != saved.size) return true
        current.zip(saved).forEach { (q, s) ->
            if (q.songId != s.song.id) return true
            if (q.source != QueueSource.SAVED) return true
        }
        return false
    }

    private companion object {
        const val POSITION_STEP = 1000
    }
}
