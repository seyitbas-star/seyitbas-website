package com.example.mindme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindme.data.AppDatabase
import com.example.mindme.data.Category
import com.example.mindme.data.RepeatType
import com.example.mindme.data.SettingsManager
import com.example.mindme.data.Task
import com.example.mindme.data.TaskLog
import com.example.mindme.data.TaskRepository
import com.example.mindme.data.TaskStatus
import com.example.mindme.data.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {


    private val settingsManager = SettingsManager(application)
    private val repository: TaskRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao(), db.taskLogDao())

        // Uygulama açılışında zamanı geçen görevleri kontrol et
        viewModelScope.launch {
            try {
                checkOverdueTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Zamanı geçen görevleri kontrol eden fonksiyon
    private suspend fun checkOverdueTasks() {
        try {
            val allTasks = repository.allTasks.first()
            val now = LocalDateTime.now()

            for (task in allTasks) {
                if (task.status == TaskStatus.PENDING && task.dueDateTime.isBefore(now)) {
                    // 1. TEKRARLAYAN GÖREV İSE (Günlük, Haftalık vs.)
                    if (task.repeatType != RepeatType.NONE) {
                        val nextDate = ReminderScheduler.getNextOccurrence(task.dueDateTime, task.repeatType)
                        if (nextDate != null) {
                            // 1. Geçmişe "Yapılmadı" olarak log düş
                            repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.MISSED))

                            // 2. Görevi bir sonraki tarihe güncelle (PENDING kalır)
                            val updatedTask = task.copy(dueDateTime = nextDate)
                            repository.update(updatedTask)

                            // 3. Yeni tarih için alarmı yeniden kur
                            ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
                        }
                    } else {
                        val updatedTask = task.copy(status = TaskStatus.MISSED)
                        repository.update(updatedTask)
                        repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.MISSED))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs: StateFlow<List<TaskLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String,
                description: String = "", // ← YENİ
                dueDateTime: LocalDateTime,
                repeatType: RepeatType = RepeatType.NONE,
                reminderMinutesBefore: Int = 2,
                category: Category = Category.NONE
    ) {
        viewModelScope.launch {
            try {
                // 1. Görevi veritabanına kaydet
                val newTask = Task(title = title, description = description, dueDateTime = dueDateTime, repeatType = repeatType, reminderMinutesBefore = reminderMinutesBefore, category = category)
                repository.insert(newTask)

                // 2. Alarmı planla
                ReminderScheduler.scheduleReminder(getApplication<Application>(), newTask)

                // 3. TAKVİM KONTROLÜ
                val settingsManager = com.example.mindme.data.SettingsManager(getApplication())
                val shouldAddToCalendar = when (category) {
                    Category.WORK -> settingsManager.autoCalendarWork.first()
                    Category.PERSONAL -> settingsManager.autoCalendarPersonal.first()
                    Category.SHOPPING -> settingsManager.autoCalendarShopping.first()
                    Category.NONE -> false
                }

                // 4. Log ile kontrol et
                android.util.Log.d("MindMe", "Görev Eklendi: ${newTask.title}, Kategori: ${newTask.category}, Takvime Eklenecek mi?: $shouldAddToCalendar")

                // 5. Takvime ekle
                if (shouldAddToCalendar) {
                    com.example.mindme.data.notification.CalendarHelper.addTaskToCalendar(getApplication(), newTask)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try { repository.update(task) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateTaskTitle(task: Task, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            try { repository.update(task.copy(title = newTitle.trim())) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                ReminderScheduler.cancelReminder(getApplication<Application>(), task)
                repository.delete(task)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun markAsCompleted(task: Task) {
        viewModelScope.launch {
            try {
                if (task.repeatType == RepeatType.NONE) {
                    repository.update(task.copy(status = TaskStatus.COMPLETED))
                    ReminderScheduler.cancelReminder(getApplication<Application>(), task)
                    repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.COMPLETED))
                } else {
                    val nextDate = ReminderScheduler.getNextOccurrence(task.dueDateTime, task.repeatType)
                    if (nextDate != null) {
                        val updatedTask = task.copy(dueDateTime = nextDate, status = TaskStatus.PENDING)
                        repository.update(updatedTask)
                        ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
                        repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.COMPLETED))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun markAsMissed(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task.copy(status = TaskStatus.MISSED))
                ReminderScheduler.cancelReminder(getApplication<Application>(), task)
                repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.MISSED))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun markAsPending(task: Task, newDateTime: LocalDateTime) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(status = TaskStatus.PENDING, dueDateTime = newDateTime)
                repository.update(updatedTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateTaskDate(task: Task, newDate: java.time.LocalDate) {
        viewModelScope.launch {
            try {
                val newDateTime = task.dueDateTime.withYear(newDate.year).withMonth(newDate.monthValue).withDayOfMonth(newDate.dayOfMonth)
                val updatedTask = task.copy(dueDateTime = newDateTime, status = TaskStatus.PENDING)
                repository.update(updatedTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateTaskTime(task: Task, newHour: Int, newMinute: Int) {
        viewModelScope.launch {
            try {
                val newDateTime = task.dueDateTime.withHour(newHour).withMinute(newMinute)
                val updatedTask = task.copy(dueDateTime = newDateTime, status = TaskStatus.PENDING)
                repository.update(updatedTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}