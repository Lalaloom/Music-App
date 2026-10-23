package com.cameronbatten.dadmusic.ui.lyricseditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.Lyrics
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.LyricsSource
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.lyrics.LrcCacheStore
import com.cameronbatten.dadmusic.lyrics.LrcParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lyricsDao: LyricsDao,
    private val songDao: SongDao,
    private val cacheStore: LrcCacheStore,
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: -1L

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song.asStateFlow()

    val existingLyrics: StateFlow<Lyrics?> = lyricsDao.observeBySongId(songId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { _song.value = songDao.findById(songId) }
    }

    /**
     * Persist [text] as a USER_EDIT [Lyrics] row for this song and update the
     * Song's lyricsStatus. Rescans with [com.cameronbatten.dadmusic.data.scan.LibraryImporter]
     * check source=USER_EDIT and leave the row untouched (Review Focus #5).
     */
    suspend fun save(text: String) {
        val target = _song.value ?: songDao.findById(songId) ?: return
        val parsed = LrcParser.parse(text)
        val hasTimings = parsed.any { it.isSynced }
        val cachePath = runCatching { cacheStore.write(songId, text) }.getOrNull()

        lyricsDao.upsert(
            Lyrics(
                songId = songId,
                rawLrc = text,
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
}
