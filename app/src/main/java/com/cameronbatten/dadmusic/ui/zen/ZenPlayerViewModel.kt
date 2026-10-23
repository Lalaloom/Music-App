package com.cameronbatten.dadmusic.ui.zen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.Lyrics
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.lyrics.LrcParser
import com.cameronbatten.dadmusic.lyrics.TimedLine
import com.cameronbatten.dadmusic.pedal.PedalAction
import com.cameronbatten.dadmusic.pedal.PedalEventBus
import com.cameronbatten.dadmusic.playback.PlaybackSession
import com.cameronbatten.dadmusic.playback.PlaybackState
import com.cameronbatten.dadmusic.playback.PositionTicker
import com.cameronbatten.dadmusic.playback.SongLookup
import com.cameronbatten.dadmusic.preferences.FontSizePreset
import com.cameronbatten.dadmusic.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZenPlayerViewModel @Inject constructor(
    private val playbackSession: PlaybackSession,
    private val songLookup: SongLookup,
    private val lyricsDao: LyricsDao,
    private val userPreferences: UserPreferences,
    positionTicker: PositionTicker,
    pedalBus: PedalEventBus,
) : ViewModel() {

    init {
        viewModelScope.launch {
            pedalBus.events.collect { action ->
                when (action) {
                    PedalAction.PLAY_PAUSE -> playPause()
                    PedalAction.NEXT -> skipNext()
                    PedalAction.PREV -> skipPrevious()
                    PedalAction.SCROLL_UP, PedalAction.SCROLL_DOWN -> {
                        // Zen view auto-scrolls with playback; manual scroll
                        // bindings are intended for the chord-sheet viewer
                        // and are ignored here.
                    }
                }
            }
        }
    }

    val playbackState: StateFlow<PlaybackState> = playbackSession.state()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSong: StateFlow<Song?> = playbackState
        .flatMapLatest { ps -> flow { emit(ps.currentSongId?.let { songLookup.findById(it) }) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val lyrics: StateFlow<Lyrics?> = currentSong
        .flatMapLatest { s -> s?.let { lyricsDao.observeBySongId(it.id) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val parsedLines: StateFlow<List<TimedLine>> = lyrics
        .map { it?.rawLrc?.let { lrc -> LrcParser.parse(lrc) } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lyricOffsetMs: StateFlow<Long> = userPreferences.lyricOffsetMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val fontSize: StateFlow<FontSizePreset> = userPreferences.fontSizePreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FontSizePreset.MEDIUM)

    private val positionMs: StateFlow<Long> = positionTicker.ticks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val currentLineIndex: StateFlow<Int> = combine(
        parsedLines, positionMs, lyricOffsetMs, playbackState,
    ) { lines, pos, offset, state ->
        computeCurrentLineIndex(lines, pos + offset, state.durationMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    fun playPause() {
        if (playbackState.value.isPlaying) playbackSession.pause() else playbackSession.resume()
    }

    fun skipNext() = playbackSession.skipNext()
    fun skipPrevious() = playbackSession.skipPrevious()
    fun seek(ms: Long) = playbackSession.seekTo(ms)
}

/**
 * Pure function: given parsed [lines], an effective position in ms (already
 * offset-adjusted), and the song's total [durationMs], return the index of the
 * currently-visible line.
 *
 * - Empty lines → -1 (caller renders a "no lyrics" placeholder).
 * - All unsynced (`startMs == -1L`) → distribute evenly across duration;
 *   blank-text entries still take a full slot but render as empty.
 * - Synced → the latest line whose startMs ≤ effectiveMs (binary search).
 */
internal fun computeCurrentLineIndex(
    lines: List<TimedLine>,
    effectiveMs: Long,
    durationMs: Long,
): Int {
    if (lines.isEmpty()) return -1
    val allUnsynced = lines.all { !it.isSynced }
    if (allUnsynced) {
        if (durationMs <= 0) return 0
        val n = lines.size
        val perLine = (durationMs / n).coerceAtLeast(1)
        return (effectiveMs / perLine).toInt().coerceIn(0, n - 1)
    }
    // Binary search for the latest startMs <= effectiveMs.
    var lo = 0
    var hi = lines.size - 1
    var ans = 0
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        val t = lines[mid].startMs
        if (t <= effectiveMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return ans
}
