package com.cameronbatten.dadmusic.data.db

import androidx.room.TypeConverter

class LyricsStatusConverter {
    @TypeConverter
    fun fromStatus(value: LyricsStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): LyricsStatus = LyricsStatus.valueOf(value)
}

class LyricsSourceConverter {
    @TypeConverter
    fun fromSource(value: LyricsSource): String = value.name

    @TypeConverter
    fun toSource(value: String): LyricsSource = LyricsSource.valueOf(value)
}
