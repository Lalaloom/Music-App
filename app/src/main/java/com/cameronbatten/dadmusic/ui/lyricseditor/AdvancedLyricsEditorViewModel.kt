package com.cameronbatten.dadmusic.ui.lyricseditor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cameronbatten.dadmusic.data.db.Lyrics
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.LyricsSource
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.lyrics.LrcCacheStore
import com.cameronbatten.dadmusic.lyrics.LrcParser
import com.cameronbatten.dadmusic.lyrics.TimedLine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One editable line in the advanced lyric editor. */
data class EditableLine(
    val text: String,
    /** Milliseconds from song start, or `null` when the line is untimed. */
    val timestampMs: Long?,
)

/**
 * Advanced lyric editor backing: owns an [ExoPlayer] scoped to the editor
 * (independent of the app's playback service), parses existing LRC into
 * editable lines, lets the user tap-to-time while playing, and saves back
 * as a `source = USER_EDIT` [Lyrics] row.
 */
@HiltViewModel
class AdvancedLyricsEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val lyricsDao: LyricsDao,
    private val songDao: SongDao,
    private val cacheStore: LrcCacheStore,
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: -1L

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song.asStateFlow()

    private val _lines = MutableStateFlow<List<EditableLine>>(emptyList())
    val lines: StateFlow<List<EditableLine>> = _lines.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().also { ex ->
        ex.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                _durationMs.value = ex.duration.coerceAtLeast(0L)
            }
        })
    }

    init {
        viewModelScope.launch {
            val s = songDao.findById(songId) ?: return@launch
            _song.value = s

            val existing = lyricsDao.findBySongId(songId)
            _lines.value = buildInitialLines(existing?.rawLrc.orEmpty())

            runCatching {
                player.setMediaItem(MediaItem.fromUri(Uri.parse(s.uri)))
                player.prepare()
            }
        }
        // Position ticker.
        viewModelScope.launch {
            while (true) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                delay(100)
            }
        }
    }

    private fun buildInitialLines(raw: String): List<EditableLine> {
        if (raw.isBlank()) return listOf(EditableLine(text = "", timestampMs = null))
        val parsed: List<TimedLine> = LrcParser.parse(raw)
        return parsed.map { tl ->
            EditableLine(
                text = tl.text,
                timestampMs = if (tl.isSynced) tl.startMs else null,
            )
        }
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceIn(0L, _durationMs.value.coerceAtLeast(0L)))
    }

    /** Rewinds 2 s and plays (handy mid-timing). */
    fun rewind2sAndPlay() {
        val target = (player.currentPosition - 2_000L).coerceAtLeast(0L)
        player.seekTo(target)
        player.play()
    }

    fun setLineText(index: Int, text: String) {
        _lines.update { it.mapIndexed { i, l -> if (i == index) l.copy(text = text) else l } }
    }

    fun stampLineAtCurrentPosition(index: Int) {
        val stamp = player.currentPosition.coerceAtLeast(0L)
        _lines.update { it.mapIndexed { i, l -> if (i == index) l.copy(timestampMs = stamp) else l } }
    }

    fun clearLineStamp(index: Int) {
        _lines.update { it.mapIndexed { i, l -> if (i == index) l.copy(timestampMs = null) else l } }
    }

    fun insertLineAfter(index: Int) {
        _lines.update {
            val head = it.subList(0, (index + 1).coerceAtMost(it.size))
            val tail = it.subList((index + 1).coerceAtMost(it.size), it.size)
            head + EditableLine(text = "", timestampMs = null) + tail
        }
    }

    fun removeLine(index: Int) {
        _lines.update { it.filterIndexed { i, _ -> i != index } }
    }

    /**
     * Build an LRC string from [lines] and persist it as a USER_EDIT [Lyrics]
     * row — same shape as the simple [LyricsEditorViewModel.save].
     */
    suspend fun save() {
        val target = _song.value ?: return
        val lrc = buildLrcText(_lines.value)
        val hasTimings = _lines.value.any { it.timestampMs != null }
        val cachePath = runCatching { cacheStore.write(songId, lrc) }.getOrNull()

        lyricsDao.upsert(
            Lyrics(
                songId = songId,
                rawLrc = lrc,
                hasTimings = hasTimings,
                source = LyricsSource.USER_EDIT,
                modifiedUtc = System.currentTimeMillis(),
            ),
        )
        songDao.upsert(
            target.copy(
                lyricsStatus = if (hasTimings) LyricsStatus.FETCHED_SYNCED else LyricsStatus.FETCHED_UNSYNCED,
                lyricsCachePath = cachePath ?: target.lyricsCachePath,
            ),
        )
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private fun MutableStateFlow<List<EditableLine>>.update(block: (List<EditableLine>) -> List<EditableLine>) {
        value = block(value)
    }

    companion object {
        fun buildLrcText(lines: List<EditableLine>): String = lines.joinToString("\n") { line ->
            val stamp = line.timestampMs?.let { formatTimestamp(it) }.orEmpty()
            "${stamp}${line.text}"
        }

        private fun formatTimestamp(ms: Long): String {
            val minutes = ms / 60_000
            val seconds = (ms % 60_000) / 1_000
            val centis = (ms % 1_000) / 10
            return "[%02d:%02d.%02d]".format(minutes, seconds, centis)
        }
    }
}
