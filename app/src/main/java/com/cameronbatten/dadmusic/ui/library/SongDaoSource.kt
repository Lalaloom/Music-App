package com.cameronbatten.dadmusic.ui.library

import com.cameronbatten.dadmusic.data.db.Song
import com.cameronbatten.dadmusic.data.db.SongDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Production [SongSource] that delegates to [SongDao.observeAll].
 *
 * Kept as a thin wrapper so [LibraryViewModel] depends on the narrower [SongSource]
 * interface and can be tested without a real Room database.
 */
class SongDaoSource @Inject constructor(
    private val songDao: SongDao,
) : SongSource {
    override fun observeAll(): Flow<List<Song>> = songDao.observeAll()
}
