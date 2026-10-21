package com.cameronbatten.dadmusic.ui.folders

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.folder.FolderStore
import com.cameronbatten.dadmusic.data.scan.ImportResult
import com.cameronbatten.dadmusic.data.scan.LibraryImporterSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [FolderPickerScreen].
 *
 * Exposes the live list of registered music folder [Uri]s and orchestrates folder
 * add/remove and library rescan operations.
 *
 * Note: [com.cameronbatten.dadmusic.playback.PlaybackController.release] is NOT wired here —
 * that responsibility belongs to a future task (Task 10 or 11) that actually consumes
 * [PlaybackController].
 */
@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val folderStore: FolderStore,
    private val libraryImporter: LibraryImporterSource,
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    /**
     * The current list of registered folder [Uri]s, ordered by insertion (LinkedHashSet order
     * preserved by [FolderRegistry]).
     *
     * Exposed as a `val` so that recomposition does not create a new upstream [stateIn]
     * subscription (and transient empty-state flash) on every call.
     */
    val folders: StateFlow<List<Uri>> = folderStore.observe()
        .map { set -> set.toList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Registers [uri] as a new music folder. */
    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            folderStore.add(uri)
        }
    }

    /** Removes [uri] from the registry. */
    fun removeFolder(uri: Uri) {
        viewModelScope.launch {
            folderStore.remove(uri)
        }
    }

    /**
     * Triggers a full library rescan.
     *
     * Sets [isImporting] to `true` for the duration, then publishes the [ImportResult].
     * Clears any prior result immediately so the UI doesn't show stale data while scanning.
     */
    fun rescan() {
        viewModelScope.launch {
            _importResult.value = null
            _isImporting.value = true
            try {
                _importResult.value = libraryImporter.importAll()
            } finally {
                _isImporting.value = false
            }
        }
    }
}
