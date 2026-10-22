package com.cameronbatten.dadmusic.ui.library

import com.cameronbatten.dadmusic.data.db.Song
import kotlinx.coroutines.flow.Flow

/**
 * Narrow read-only interface over the song list.
 *
 * Allows [LibraryViewModel] to be tested without a real Room database.
 * The production binding wires [com.cameronbatten.dadmusic.data.db.SongDao] via
 * [com.cameronbatten.dadmusic.di.LibraryBindsModule].
 */
interface SongSource {
    /** Emits the full list of songs ordered by title ASC. */
    fun observeAll(): Flow<List<Song>>
}
