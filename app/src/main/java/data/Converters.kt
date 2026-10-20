package com.example.mindme.data

import androidx.room.TypeConverter
import java.time.LocalDateTime

class Converters {

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateTimeToTimestamp(dateTime: LocalDateTime?): String? {
        return dateTime?.toString()
    }
    @TypeConverter
    fun fromStatus(value: String?): TaskStatus? {
        return value?.let { TaskStatus.valueOf(it) }
    }

    @TypeConverter
    fun statusToString(status: TaskStatus?): String? {
        return status?.name
    }

}