package com.cameronbatten.dadmusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Song::class], version = 2, exportSchema = false)
@TypeConverters(FileTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
