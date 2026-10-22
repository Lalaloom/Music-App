package com.cameronbatten.dadmusic.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.QueueSource
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.setlist.AppendResult
import com.cameronbatten.dadmusic.setlist.QueueAppender
import com.cameronbatten.dadmusic.ui.library.SongSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI-facing outcome of a search-pane add. Carries the human-readable title so
 * the Snackbar message can be composed without a second DB lookup.
 */
sealed interface AddToQueueOutcome {
    data class Added(val position: Int, val title: String) : AddToQueueOutcome
    data class AlreadyPresent(val title: String) : AddToQueueOutcome
}

/**
 * Backs [SearchPane]. Owns search text, filtered results, and the add-to-queue
 * action. Does NOT own undo state — that lives in [NavigatorScreen]'s Snackbar
 * host so the message surface stays at the navigator level.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    songSource: SongSource,
    private val queueAppender: QueueAppender,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Case-insensitive substring filter over title + artist. */
    val results: StateFlow<List<Song>> = combine(songSource.observeAll(), _query) { songs, q ->
        if (q.isBlank()) {
            songs
        } else {
            val needle = q.trim().lowercase()
            songs.filter { s ->
                s.title.lowercase().contains(needle) ||
                    s.artist?.lowercase()?.contains(needle) == true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(q: String) {
        _query.value = q
    }

    suspend fun addToQueue(song: Song): AddToQueueOutcome =
        when (val r = queueAppender.append(song.id, QueueSource.REQUEST)) {
            is AppendResult.Added -> AddToQueueOutcome.Added(r.entry.position, song.title)
            is AppendResult.AlreadyPresent -> AddToQueueOutcome.AlreadyPresent(song.title)
        }
}
