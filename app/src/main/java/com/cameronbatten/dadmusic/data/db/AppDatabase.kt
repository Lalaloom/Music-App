package com.cameronbatten.dadmusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Song::class,
        Lyrics::class,
        Setlist::class,
        SetlistEntry::class,
        LiveQueueEntry::class,
        ChordSheet::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(
    FileTypeConverter::class,
    LyricsStatusConverter::class,
    LyricsSourceConverter::class,
    QueueSourceConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun setlistDao(): SetlistDao
    abstract fun liveQueueDao(): LiveQueueDao
    abstract fun chordSheetDao(): ChordSheetDao
}
