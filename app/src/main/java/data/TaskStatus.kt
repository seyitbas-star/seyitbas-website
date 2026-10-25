package com.example.mindme.data

enum class TaskStatus {
    PENDING,
    COMPLETED,
    MISSED
}
enum class RepeatType {
    NONE,       // Tek seferlik
    DAILY,      // Her gün
    WEEKLY,     // Her hafta
    MONTHLY,    // Her ay
    YEARLY      // Her yıl (doğum günü)
}
enum class Category(val displayName: String) {
    NONE("Genel"),
    WORK("İş"),
    PERSONAL("Kişisel"),
    SHOPPING("Alışveriş")
}