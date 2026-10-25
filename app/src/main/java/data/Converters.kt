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
    @TypeConverter
    fun fromRepeatType(value: String?): RepeatType? {
        return value?.let { RepeatType.valueOf(it) }
    }

    @TypeConverter
    fun repeatTypeToString(repeatType: RepeatType?): String? {
        return repeatType?.name
    }
    @TypeConverter
    fun fromCategory(value: String?): Category? {
        return value?.let { Category.valueOf(it) }
    }

    @TypeConverter
    fun categoryToString(category: Category?): String? {
        return category?.name
    }
}