package com.cameronbatten.dadmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    /**
     * Insert or replace the lyrics for a song. Uniqueness is enforced on `songId`
     * so a REPLACE conflict here is intentional — refetches and user edits land
     * on the same row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lyrics: Lyrics): Long

    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    suspend fun findBySongId(songId: Long): Lyrics?

    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    fun observeBySongId(songId: Long): Flow<Lyrics?>

    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun deleteBySongId(songId: Long)

    /** Admin → Clear lyrics cache: wipes every lyrics row in one shot. */
    @Query("DELETE FROM lyrics")
    suspend fun deleteAll(): Int
}
