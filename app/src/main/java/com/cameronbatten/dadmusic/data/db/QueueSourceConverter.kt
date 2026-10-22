package com.cameronbatten.dadmusic.data.db

import androidx.room.TypeConverter

class QueueSourceConverter {
    @TypeConverter
    fun fromSource(value: QueueSource): String = value.name

    @TypeConverter
    fun toSource(value: String): QueueSource = QueueSource.valueOf(value)
}
