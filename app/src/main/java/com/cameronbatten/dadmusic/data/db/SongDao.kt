package com.cameronbatten.dadmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cameronbatten.dadmusic.playback.SongLookup
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao : SongLookup {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song): Long

    /**
     * Insert if new, UPDATE in place if the primary key matches an existing row.
     *
     * Unlike [insert] (which uses `OnConflictStrategy.REPLACE` and therefore performs a
     * `DELETE + INSERT` sequence), [upsert] uses `INSERT OR IGNORE` followed by `UPDATE`,
     * which **does not fire `ON DELETE CASCADE` on child tables**. That matters here
     * because the `lyrics` table has an FK on `songId` with `CASCADE` delete: a rescan
     * that touched existing songs via REPLACE would silently wipe their fetched lyrics.
     * Use this method for every scan-driven upsert of an existing song.
     */
    @Upsert
    suspend fun upsert(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>): List<Long>

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun observeAll(): Flow<List<Song>>

    @Query("SELECT uri FROM songs")
    suspend fun allUris(): List<String>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<Song>

    @Query("DELETE FROM songs WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("SELECT * FROM songs WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): Song?

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    override suspend fun findById(id: Long): Song?
}
