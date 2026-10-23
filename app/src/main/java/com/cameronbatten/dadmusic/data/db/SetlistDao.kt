package com.cameronbatten.dadmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SetlistDao {

    @Upsert
    abstract suspend fun upsert(setlist: Setlist): Long

    @Query("SELECT * FROM setlists ORDER BY orderIndex ASC, createdAt ASC")
    abstract fun observeAll(): Flow<List<Setlist>>

    @Query("SELECT * FROM setlists ORDER BY orderIndex ASC, createdAt ASC")
    abstract suspend fun getAll(): List<Setlist>

    @Query("SELECT * FROM setlists WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): Setlist?

    @Query("SELECT * FROM setlists WHERE sourceFolderUri = :uri LIMIT 1")
    abstract suspend fun findByFolder(uri: String): Setlist?

    @Query("DELETE FROM setlists WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEntries(entries: List<SetlistEntry>)

    @Query("DELETE FROM setlist_entries WHERE setlistId = :setlistId")
    abstract suspend fun deleteEntriesFor(setlistId: Long)

    @Query(
        """
        SELECT songs.*, setlist_entries.position AS position
        FROM songs INNER JOIN setlist_entries ON songs.id = setlist_entries.songId
        WHERE setlist_entries.setlistId = :setlistId
        ORDER BY setlist_entries.position ASC
        """,
    )
    abstract suspend fun entriesFor(setlistId: Long): List<SongWithPosition>

    @Query(
        """
        SELECT songs.*, setlist_entries.position AS position
        FROM songs INNER JOIN setlist_entries ON songs.id = setlist_entries.songId
        WHERE setlist_entries.setlistId = :setlistId
        ORDER BY setlist_entries.position ASC
        """,
    )
    abstract fun observeEntries(setlistId: Long): Flow<List<SongWithPosition>>

    /**
     * Atomically wipes the current entries for [setlistId] and inserts [entries].
     * Callers constructing [entries] should use sparse integer positions
     * (1000, 2000, 3000…) so later single-row insertions can take midpoints
     * without renumbering.
     */
    @Transaction
    open suspend fun replaceEntries(setlistId: Long, entries: List<SetlistEntry>) {
        deleteEntriesFor(setlistId)
        if (entries.isNotEmpty()) insertEntries(entries)
    }
}
