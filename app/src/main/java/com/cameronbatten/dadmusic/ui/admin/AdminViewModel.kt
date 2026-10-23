package com.cameronbatten.dadmusic.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.data.scan.LibraryImporterSource
import com.cameronbatten.dadmusic.entitlements.EntitlementSource
import com.cameronbatten.dadmusic.lyrics.BulkLyricsFetchJob
import com.cameronbatten.dadmusic.lyrics.FetchProgress
import com.cameronbatten.dadmusic.lyrics.LrcCacheStore
import com.cameronbatten.dadmusic.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Admin screen state model. Mirrors the hidden power-user panel from spec §5.12.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val entitlementSource: EntitlementSource,
    private val importer: LibraryImporterSource,
    private val bulkFetchJob: BulkLyricsFetchJob,
    private val cacheStore: LrcCacheStore,
    private val lyricsDao: LyricsDao,
    private val songDao: SongDao,
    private val database: AppDatabase,
) : ViewModel() {

    /** Observed entitlements — reflects the override + base layering. */
    val entitlements = entitlementSource.entitlements

    /** Current admin override setting (null = default / passthrough). */
    val adminOverride: StateFlow<Boolean?> = prefs.adminProOverride
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Songs for the DB editor, title-sorted. */
    val songs: StateFlow<List<Song>> = songDao.observeAllByTitle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Progress of the currently-running force action (null = idle). */
    private val _work = MutableStateFlow<AdminWork?>(null)
    val work: StateFlow<AdminWork?> = _work.asStateFlow()

    private val _snack = Channel<String>(capacity = Channel.BUFFERED)
    val snack = _snack.receiveAsFlow()

    /**
     * Signals that the "Reset device state" action completed and the UI must
     * route back to the onboarding Welcome. One-shot event (not a flag).
     */
    private val _resetComplete = Channel<Unit>(capacity = 1)
    val resetComplete = _resetComplete.receiveAsFlow()

    private val workLock = Mutex()

    fun setOverride(override: Boolean?) = viewModelScope.launch {
        prefs.setAdminProOverride(override)
    }

    fun rescanNow() = runWorkOnce(AdminWork.Rescan) {
        val result = importer.importAll()
        _snack.send("Rescan done — added ${result.added}, updated ${result.updated}, removed ${result.skipped}.")
    }

    fun clearLyricsCache() = runWorkOnce(AdminWork.ClearLyrics) {
        val removed = cacheStore.clearAll()
        lyricsDao.deleteAll()
        songDao.markAllLyricsStatusMissing()
        _snack.send("Lyrics cache cleared — removed $removed files; every song is back to MISSING.")
    }

    fun refetchAllLyrics() = runWorkOnce(AdminWork.RefetchLyrics) {
        var last: FetchProgress? = null
        bulkFetchJob.run().collectLatest { last = it }
        last?.let {
            _snack.send("Lyrics re-fetch done — ${it.completed}/${it.total} succeeded; ${it.failed} failed.")
        }
    }

    fun saveSongEdit(id: Long, title: String, artist: String?, confirmed: Boolean) =
        viewModelScope.launch {
            val cleanTitle = title.trim().takeIf { it.isNotEmpty() } ?: return@launch
            val cleanArtist = artist?.trim()?.takeIf { it.isNotEmpty() }
            val changed = songDao.updateTitleArtist(id, cleanTitle, cleanArtist, confirmed)
            if (changed > 0) _snack.send("Song #$id updated.")
        }

    fun resetDeviceState() = runWorkOnce(AdminWork.ResetDevice) {
        // Order: DB tables first (rows go), lyrics cache dir second
        // (files go), then prefs. Each is idempotent — a crash between any
        // two of them is recoverable by re-tapping Reset.
        database.clearAllTables()
        cacheStore.clearAll()
        prefs.resetAllExceptFolders()
        _resetComplete.send(Unit)
    }

    private fun runWorkOnce(label: AdminWork, block: suspend () -> Unit) = viewModelScope.launch {
        if (!workLock.tryLock()) {
            _snack.send("Already running — hold on.")
            return@launch
        }
        try {
            _work.value = label
            block()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            _snack.send("Failed: ${t.message ?: t.javaClass.simpleName}")
        } finally {
            _work.value = null
            workLock.unlock()
        }
    }
}

/** Which long-running admin action (if any) is currently in flight. */
enum class AdminWork { Rescan, ClearLyrics, RefetchLyrics, ResetDevice }
