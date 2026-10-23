package com.cameronbatten.dadmusic.ui.chord

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.chord.ChordProParser
import com.cameronbatten.dadmusic.chord.ChordSheetDocument
import com.cameronbatten.dadmusic.data.db.ChordSheetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChordSheetViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val chordSheetDao: ChordSheetDao,
) : ViewModel() {

    private val sheetId: Long = savedStateHandle.get<Long>("sheetId") ?: -1L

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    private val _artist = MutableStateFlow<String?>(null)
    val artist: StateFlow<String?> = _artist.asStateFlow()

    private val _document = MutableStateFlow<ChordSheetDocument?>(null)
    val document: StateFlow<ChordSheetDocument?> = _document.asStateFlow()

    private val _transpose = MutableStateFlow(0)
    val transpose: StateFlow<Int> = _transpose.asStateFlow()

    init {
        viewModelScope.launch {
            val row = chordSheetDao.findById(sheetId) ?: return@launch
            _title.value = row.title
            _artist.value = row.artist
            val text = runCatching { readText(Uri.parse(row.uri)) }.getOrNull() ?: return@launch
            _document.value = ChordProParser.parse(text)
        }
    }

    fun transposeUp() {
        _transpose.value = _transpose.value + 1
    }

    fun transposeDown() {
        _transpose.value = _transpose.value - 1
    }

    fun resetTranspose() {
        _transpose.value = 0
    }

    private suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
