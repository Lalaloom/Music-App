package com.cameronbatten.dadmusic.ui.chord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.ChordSheet
import com.cameronbatten.dadmusic.data.db.ChordSheetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChordSheetListViewModel @Inject constructor(
    chordSheetDao: ChordSheetDao,
) : ViewModel() {
    val sheets: StateFlow<List<ChordSheet>> = chordSheetDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
