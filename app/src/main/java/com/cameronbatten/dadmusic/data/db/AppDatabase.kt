package com.cameronbatten.dadmusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Song::class, Lyrics::class], version = 3, exportSchema = false)
@TypeConverters(FileTypeConverter::class, LyricsStatusConverter::class, LyricsSourceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricsDao(): LyricsDao
}
