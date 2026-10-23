package com.cameronbatten.dadmusic.ui.pdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.PdfSheet
import com.cameronbatten.dadmusic.data.db.PdfSheetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PdfListViewModel @Inject constructor(
    pdfSheetDao: PdfSheetDao,
) : ViewModel() {
    val sheets: StateFlow<List<PdfSheet>> = pdfSheetDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
