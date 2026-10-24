package com.example.mindme.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDateTime: LocalDateTime,
    val status: TaskStatus = TaskStatus.PENDING,
    val repeatType: RepeatType = RepeatType.NONE,
    val reminderMinutesBefore: Int = 2   // ← YENİ (Varsayılan 2 dk)
)