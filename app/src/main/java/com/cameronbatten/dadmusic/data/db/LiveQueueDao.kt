package com.cameronbatten.dadmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LiveQueueDao {

    @Query("SELECT * FROM live_queue ORDER BY position ASC")
    abstract fun observeAll(): Flow<List<LiveQueueEntry>>

    @Query("SELECT * FROM live_queue ORDER BY position ASC")
    abstract suspend fun getAll(): List<LiveQueueEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entry: LiveQueueEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entries: List<LiveQueueEntry>)

    @Query("DELETE FROM live_queue WHERE position = :position")
    abstract suspend fun deleteByPosition(position: Int)

    @Query("DELETE FROM live_queue")
    abstract suspend fun deleteAll()

    @Query("SELECT MAX(position) FROM live_queue")
    abstract suspend fun maxPosition(): Int?

    @Transaction
    open suspend fun replaceAll(entries: List<LiveQueueEntry>) {
        deleteAll()
        if (entries.isNotEmpty()) insertAll(entries)
    }
}
