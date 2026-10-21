package com.cameronbatten.dadmusic.data.db

import androidx.room.TypeConverter

class FileTypeConverter {
    @TypeConverter
    fun fromFileType(value: FileType): String = value.name

    @TypeConverter
    fun toFileType(value: String): FileType = FileType.valueOf(value)
}
