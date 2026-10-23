package com.cameronbatten.dadmusic.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.data.db.PdfSheetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Current snapshot of what the viewer is rendering. */
data class PdfViewerState(
    val title: String = "",
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val bitmap: Bitmap? = null,
    val error: String? = null,
)

/**
 * PDF viewer backing. Uses `android.graphics.pdf.PdfRenderer` to render one
 * page at a time to a Bitmap; the Composable displays whatever bitmap sits in
 * [state]. Resources are released in [onCleared].
 */
@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val pdfSheetDao: PdfSheetDao,
) : ViewModel() {

    private val sheetId: Long = savedStateHandle.get<Long>("sheetId") ?: -1L

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    init {
        viewModelScope.launch {
            runCatching { load() }.onFailure { t ->
                _state.value = _state.value.copy(error = t.message ?: "Open failed")
            }
        }
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        val row = pdfSheetDao.findById(sheetId) ?: run {
            _state.value = _state.value.copy(error = "Not found")
            return@withContext
        }
        val uri = Uri.parse(row.uri)
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Could not open PDF file descriptor")
        fd = pfd
        val r = PdfRenderer(pfd)
        renderer = r
        _state.value = PdfViewerState(
            title = row.title,
            pageCount = r.pageCount,
            pageIndex = 0,
        )
        renderPage(0)
    }

    private suspend fun renderPage(index: Int) = withContext(Dispatchers.IO) {
        val r = renderer ?: return@withContext
        val clamped = index.coerceIn(0, r.pageCount - 1)
        val page = r.openPage(clamped)
        // Render at a readable size — 2x the page's reported width in px seems
        // to be a sweet spot for Android tablets. Nothing fancy.
        val w = (page.width * 2).coerceAtMost(2048)
        val h = (page.height * 2).coerceAtMost(2560)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        _state.value = _state.value.copy(pageIndex = clamped, bitmap = bmp)
    }

    fun nextPage() = viewModelScope.launch { renderPage(_state.value.pageIndex + 1) }
    fun prevPage() = viewModelScope.launch { renderPage(_state.value.pageIndex - 1) }

    override fun onCleared() {
        renderer?.close()
        fd?.close()
        super.onCleared()
    }
}
