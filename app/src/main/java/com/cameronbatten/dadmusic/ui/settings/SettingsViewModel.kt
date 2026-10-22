package com.cameronbatten.dadmusic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.LyricsStatus
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.lyrics.BulkLyricsFetchJob
import com.cameronbatten.dadmusic.lyrics.FetchProgress
import com.cameronbatten.dadmusic.preferences.EndBehaviour
import com.cameronbatten.dadmusic.preferences.FontSizePreset
import com.cameronbatten.dadmusic.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val bulkFetchJob: BulkLyricsFetchJob,
    private val songDao: SongDao,
) : ViewModel() {

    val lyricOffsetMs: StateFlow<Long> = userPreferences.lyricOffsetMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val fontSize: StateFlow<FontSizePreset> = userPreferences.fontSizePreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FontSizePreset.MEDIUM)

    val onSongEnded: StateFlow<EndBehaviour> = userPreferences.onSongEnded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EndBehaviour.PAUSE)

    private val _fetchProgress = MutableStateFlow<FetchProgress?>(null)
    val fetchProgress: StateFlow<FetchProgress?> = _fetchProgress.asStateFlow()

    private var currentFetchJob: Job? = null

    fun setLyricOffset(ms: Long) {
        viewModelScope.launch { userPreferences.setLyricOffsetMs(ms) }
    }

    fun setFontSize(preset: FontSizePreset) {
        viewModelScope.launch { userPreferences.setFontSizePreset(preset) }
    }

    fun setOnSongEnded(behaviour: EndBehaviour) {
        viewModelScope.launch { userPreferences.setOnSongEnded(behaviour) }
    }

    /**
     * Flip every FAILED song back to MISSING, then run the bulk fetch job.
     * ADOPTED_FROM_DISK and FETCHED_* stay as they are.
     */
    fun refetchAllLyrics() {
        if (currentFetchJob?.isActive == true) return
        currentFetchJob = viewModelScope.launch {
            songDao.getAll()
                .filter { it.lyricsStatus == LyricsStatus.FAILED }
                .forEach { songDao.upsert(it.copy(lyricsStatus = LyricsStatus.MISSING)) }
            bulkFetchJob.run().collect { _fetchProgress.value = it }
        }
    }
}
