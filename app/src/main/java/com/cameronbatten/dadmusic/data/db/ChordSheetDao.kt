package com.cameronbatten.dadmusic.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChordSheetDao {
    @Upsert
    suspend fun upsert(sheet: ChordSheet): Long

    @Query("SELECT * FROM chord_sheets ORDER BY title ASC")
    fun observeAll(): Flow<List<ChordSheet>>

    @Query("SELECT * FROM chord_sheets ORDER BY title ASC")
    suspend fun getAll(): List<ChordSheet>

    @Query("SELECT * FROM chord_sheets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ChordSheet?

    @Query("SELECT * FROM chord_sheets WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): ChordSheet?

    @Query("DELETE FROM chord_sheets WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM chord_sheets WHERE folderUri = :folderUri")
    suspend fun deleteByFolder(folderUri: String)
}
