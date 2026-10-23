package com.cameronbatten.dadmusic.setlist.sharing

import com.cameronbatten.dadmusic.data.db.Setlist
import com.cameronbatten.dadmusic.data.db.SetlistDao
import com.cameronbatten.dadmusic.data.db.SetlistEntry
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Result of importing a `.setlist.json`. */
data class SetlistImportResult(
    val setlistId: Long,
    val matched: Int,
    val missing: Int,
    val missingTitles: List<String>,
)

/**
 * Parses a `.setlist.json`, matches each entry against the local [SongDao] by
 * `(title, artist)` (ignoring case), and creates a fresh setlist with the
 * matched entries. Unmatched entries are reported — nothing is skipped silently.
 */
class SetlistImporter @Inject constructor(
    private val songDao: SongDao,
    private val setlistDao: SetlistDao,
) {
    suspend fun import(json: String): SetlistImportResult? = withContext(Dispatchers.IO) {
        val shared = SharedSetlistCodec.decode(json) ?: return@withContext null
        val allSongs = songDao.getAll()
        val byKey = allSongs.associateBy { (it.title.lowercase() to (it.artist?.lowercase())) }

        val matchedIds = mutableListOf<Long>()
        val missing = mutableListOf<String>()
        for (s in shared.songs) {
            val key = s.title.lowercase() to s.artist?.lowercase()
            val song = byKey[key]
            if (song != null) matchedIds += song.id
            else missing += s.title
        }

        val now = System.currentTimeMillis()
        val setlistId = setlistDao.upsert(
            Setlist(
                name = shared.name,
                sourceFolderUri = null,
                orderIndex = 0,
                createdAt = now,
                modifiedUtc = now,
            ),
        )
        val entries = matchedIds.mapIndexed { i, id ->
            SetlistEntry(setlistId = setlistId, songId = id, position = (i + 1) * 1000)
        }
        setlistDao.replaceEntries(setlistId, entries)

        SetlistImportResult(
            setlistId = setlistId,
            matched = matchedIds.size,
            missing = missing.size,
            missingTitles = missing,
        )
    }
}
