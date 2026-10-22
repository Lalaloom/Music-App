package com.cameronbatten.dadmusic.ui.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.folder.FolderRegistry
import com.cameronbatten.dadmusic.data.scan.ImportResult
import com.cameronbatten.dadmusic.data.scan.LibraryImporterSource
import com.cameronbatten.dadmusic.lyrics.BulkLyricsFetchJob
import com.cameronbatten.dadmusic.lyrics.FetchProgress
import com.cameronbatten.dadmusic.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { WELCOME, FOLDERS, SCANNING, DONE }

sealed interface ScanStage {
    data object Idle : ScanStage
    data object Scanning : ScanStage
    data class Fetching(val progress: FetchProgress) : ScanStage
    data class Finished(val result: ImportResult) : ScanStage
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val folderRegistry: FolderRegistry,
    private val libraryImporter: LibraryImporterSource,
    private val bulkLyricsFetchJob: BulkLyricsFetchJob,
) : ViewModel() {

    private val _step = MutableStateFlow(OnboardingStep.WELCOME)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    val folders: StateFlow<Set<Uri>> = folderRegistry.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _scanStage = MutableStateFlow<ScanStage>(ScanStage.Idle)
    val scanStage: StateFlow<ScanStage> = _scanStage.asStateFlow()

    fun addFolder(uri: Uri) {
        viewModelScope.launch { folderRegistry.add(uri) }
    }

    fun removeFolder(uri: Uri) {
        viewModelScope.launch { folderRegistry.remove(uri) }
    }

    fun advanceToFolders() {
        _step.value = OnboardingStep.FOLDERS
    }

    fun advanceToScanning() {
        _step.value = OnboardingStep.SCANNING
        viewModelScope.launch {
            _scanStage.value = ScanStage.Scanning
            val result = libraryImporter.importAll()
            // Chain the lyrics fetch; emits progress snapshots as it goes.
            bulkLyricsFetchJob.run().collect { _scanStage.value = ScanStage.Fetching(it) }
            _scanStage.value = ScanStage.Finished(result)
            _step.value = OnboardingStep.DONE
        }
    }

    fun complete(onFinish: () -> Unit) {
        viewModelScope.launch {
            userPreferences.markOnboardingComplete()
            onFinish()
        }
    }
}
