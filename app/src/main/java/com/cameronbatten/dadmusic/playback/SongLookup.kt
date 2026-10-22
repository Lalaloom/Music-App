package com.cameronbatten.dadmusic.playback

import com.cameronbatten.dadmusic.data.db.Song

/**
 * Narrow interface for looking up a single [Song] by its database primary key.
 *
 * [com.cameronbatten.dadmusic.data.db.SongDao] implements this via Hilt @Binds.
 * Tests inject a [com.cameronbatten.dadmusic.ui.player.FakeSongLookup] instead.
 */
interface SongLookup {
    suspend fun findById(id: Long): Song?
}
