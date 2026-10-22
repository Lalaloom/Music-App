package com.cameronbatten.dadmusic.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * A single song-in-a-setlist row.
 *
 * ### Why sparse-integer [position]
 * Spec §7.2 pins "sparse integers (1000, 2000, 3000…)". This leaves room between
 * entries so a mid-set insertion (e.g., a request-add while a saved setlist is
 * open) can take an integer midpoint without renumbering the whole table. When
 * gaps collapse the DAO's `replaceEntries` call renumbers everything from scratch.
 *
 * ### Why CASCADE on both parents
 * Deleting a Setlist deletes its entries. Deleting a Song deletes any
 * SetlistEntry pointing to it. Both are the correct user-expectation outcomes.
 */
@Entity(
    tableName = "setlist_entries",
    primaryKeys = ["setlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Setlist::class,
            parentColumns = ["id"],
            childColumns = ["setlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("songId"), Index("setlistId")],
)
data class SetlistEntry(
    val setlistId: Long,
    val songId: Long,
    val position: Int,
)
