package com.cameronbatten.dadmusic.setlist.sharing

import com.cameronbatten.dadmusic.data.db.SetlistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Pulls a setlist + its entries out of the DB and encodes them as JSON ready
 * for ACTION_CREATE_DOCUMENT.
 */
class SetlistExporter @Inject constructor(
    private val setlistDao: SetlistDao,
) {
    suspend fun export(setlistId: Long): String? = withContext(Dispatchers.IO) {
        val setlist = setlistDao.findById(setlistId) ?: return@withContext null
        val entries = setlistDao.entriesFor(setlistId)
        val shared = SharedSetlist(
            name = setlist.name,
            songs = entries.map { SharedSetlistSong(it.song.title, it.song.artist) },
        )
        SharedSetlistCodec.encode(shared)
    }
}
