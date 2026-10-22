package com.cameronbatten.dadmusic.ui.navigator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.LiveQueueEntry
import com.cameronbatten.dadmusic.data.db.Setlist
import com.cameronbatten.dadmusic.data.db.SetlistDao
import com.cameronbatten.dadmusic.data.db.SetlistEntry
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.playback.PlaybackSession
import com.cameronbatten.dadmusic.playback.PlaybackState
import com.cameronbatten.dadmusic.playback.SongLookup
import com.cameronbatten.dadmusic.setlist.LiveQueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pairs a [LiveQueueEntry] with the resolved [Song] so the UI can render both
 * title/artist and the position/source bits in one shot.
 */
data class SongInQueue(val entry: LiveQueueEntry, val song: Song)

/**
 * Backs [NavigatorScreen]. Owns:
 *
 * - The list of setlists for the dropdown.
 * - The live queue, materialised into [SongInQueue] via an in-memory join
 *   against the full song list observer (one DB subscription, not N findById
 *   calls per emission).
 * - Divergence state so the "Save changes to <setlist>" button only appears
 *   when the current queue differs from the saved template.
 * - Live playback state so the mini-player and "NOW PLAYING" row marker stay
 *   in sync.
 */
@HiltViewModel
class NavigatorViewModel @Inject constructor(
    private val setlistDao: SetlistDao,
    songDao: SongDao,
    private val liveQueueManager: LiveQueueManager,
    private val playbackSession: PlaybackSession,
    songLookup: SongLookup,
) : ViewModel() {

    val setlists: StateFlow<List<Setlist>> = setlistDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeSetlistId = MutableStateFlow<Long?>(null)
    val activeSetlistId: StateFlow<Long?> = _activeSetlistId.asStateFlow()

    private val _hasDiverged = MutableStateFlow(false)
    val hasDiverged: StateFlow<Boolean> = _hasDiverged.asStateFlow()

    /**
     * In-memory join of the live-queue table against the full song list.
     * Beats N `findById` calls per emission for a queue of any non-trivial size.
     */
    val queue: StateFlow<List<SongInQueue>> =
        combine(liveQueueManager.observe(), songDao.observeAll()) { entries, songs ->
            val byId = songs.associateBy { it.id }
            entries.mapNotNull { e -> byId[e.songId]?.let { SongInQueue(e, it) } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playbackSession.state()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSong: StateFlow<Song?> = playbackState
        .flatMapLatest { ps -> flow { emit(ps.currentSongId?.let { songLookup.findById(it) }) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            liveQueueManager.observe().collect {
                _activeSetlistId.value = liveQueueManager.currentParentSetlistId()
                _hasDiverged.value = liveQueueManager.hasDivergedFromParent()
            }
        }
    }

    fun switchTo(setlistId: Long?) {
        viewModelScope.launch {
            if (setlistId == null) liveQueueManager.clear()
            else liveQueueManager.openSetlist(setlistId)
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { liveQueueManager.moveByIndex(fromIndex, toIndex) }
    }

    /** Persist a drag-reorder's final order in one write. */
    fun applyQueueOrder(songIds: List<Long>) {
        viewModelScope.launch { liveQueueManager.applyOrder(songIds) }
    }

    fun removeFromQueue(position: Int) {
        viewModelScope.launch { liveQueueManager.remove(position) }
    }

    fun saveQueueToParent() {
        viewModelScope.launch {
            val parentId = liveQueueManager.currentParentSetlistId() ?: return@launch
            val snapshot = liveQueueManager.snapshot()
            val entries = snapshot.mapIndexed { i, lqe ->
                SetlistEntry(
                    setlistId = parentId,
                    songId = lqe.songId,
                    position = (i + 1) * 1000,
                )
            }
            setlistDao.replaceEntries(parentId, entries)
            // The queue is now identical to the saved template → divergence resets.
            _hasDiverged.value = false
        }
    }

    fun playSongAt(position: Int) {
        viewModelScope.launch {
            val song = queue.value.firstOrNull { it.entry.position == position }?.song
                ?: return@launch
            playbackSession.play(song)
        }
    }

    fun playPause() {
        if (playbackState.value.isPlaying) playbackSession.pause() else playbackSession.resume()
    }
}
