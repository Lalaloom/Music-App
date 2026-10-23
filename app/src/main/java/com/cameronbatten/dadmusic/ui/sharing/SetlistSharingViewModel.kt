package com.cameronbatten.dadmusic.ui.sharing

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.Setlist
import com.cameronbatten.dadmusic.data.db.SetlistDao
import com.cameronbatten.dadmusic.setlist.sharing.SetlistExporter
import com.cameronbatten.dadmusic.setlist.sharing.SetlistImportResult
import com.cameronbatten.dadmusic.setlist.sharing.SetlistImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SetlistSharingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: SetlistExporter,
    private val importer: SetlistImporter,
    setlistDao: SetlistDao,
) : ViewModel() {

    val setlists: StateFlow<List<Setlist>> = setlistDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    private val _lastImport = MutableStateFlow<SetlistImportResult?>(null)
    val lastImport: StateFlow<SetlistImportResult?> = _lastImport.asStateFlow()

    /**
     * Writes an exported setlist's JSON into the SAF URI the user picked from
     * ACTION_CREATE_DOCUMENT.
     */
    fun exportTo(setlistId: Long, destination: Uri) = viewModelScope.launch {
        val json = exporter.export(setlistId) ?: run {
            _lastMessage.value = "Setlist not found."
            return@launch
        }
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(destination)?.use {
                it.write(json.toByteArray(Charsets.UTF_8))
            }
        }
        _lastMessage.value = "Exported."
    }

    /** Reads and imports a `.setlist.json` from an ACTION_OPEN_DOCUMENT URI. */
    fun importFrom(source: Uri) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(source)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        } ?: run {
            _lastMessage.value = "Could not open file."
            return@launch
        }
        val result = importer.import(text)
        if (result == null) {
            _lastMessage.value = "File is not a valid setlist export."
            return@launch
        }
        _lastImport.value = result
        _lastMessage.value = "Imported — matched ${result.matched}, missing ${result.missing}."
    }

    fun clearMessage() {
        _lastMessage.value = null
    }
}
