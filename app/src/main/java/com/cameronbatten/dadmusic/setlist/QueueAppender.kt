package com.cameronbatten.dadmusic.setlist

import com.cameronbatten.dadmusic.data.db.LiveQueueEntry
import com.cameronbatten.dadmusic.data.db.QueueSource

/**
 * Outcome of a [QueueAppender.append] call. Lets callers distinguish an actual
 * insert (undoable) from a no-op where the song was already in the queue
 * (info-only snackbar).
 */
sealed interface AppendResult {
    val entry: LiveQueueEntry

    data class Added(override val entry: LiveQueueEntry) : AppendResult
    data class AlreadyPresent(override val entry: LiveQueueEntry) : AppendResult
}

/**
 * Narrow interface for appending a song to the live queue. [LiveQueueManager]
 * implements it; the search feature depends on only this so tests can inject
 * a lightweight fake without standing up a Room DB.
 */
interface QueueAppender {
    suspend fun append(songId: Long, source: QueueSource): AppendResult
}
