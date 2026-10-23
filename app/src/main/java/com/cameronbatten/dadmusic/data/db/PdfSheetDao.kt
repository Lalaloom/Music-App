package com.cameronbatten.dadmusic.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfSheetDao {
    @Upsert
    suspend fun upsert(sheet: PdfSheet): Long

    @Query("SELECT * FROM pdf_sheets ORDER BY title ASC")
    fun observeAll(): Flow<List<PdfSheet>>

    @Query("SELECT * FROM pdf_sheets ORDER BY title ASC")
    suspend fun getAll(): List<PdfSheet>

    @Query("SELECT * FROM pdf_sheets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PdfSheet?

    @Query("DELETE FROM pdf_sheets WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}
