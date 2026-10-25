package com.example.mindme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindme.data.AppDatabase
import com.example.mindme.data.Category
import com.example.mindme.data.RepeatType
import com.example.mindme.data.Task
import com.example.mindme.data.TaskLog
import com.example.mindme.data.TaskRepository
import com.example.mindme.data.TaskStatus
import com.example.mindme.data.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    init {
        val db = AppDatabase.getDatabase(application)
        // Repository'ye iki DAO'yu da veriyoruz
        repository = TaskRepository(db.taskDao(), db.taskLogDao())

        // Uygulama açılışında zamanı geçen görevleri işaretle
        viewModelScope.launch {
            try {
                repository.markOverdueTasksAsMissed()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String,
                dueDateTime: LocalDateTime,
                repeatType: RepeatType = RepeatType.NONE,
                reminderMinutesBefore: Int = 2,
                category: Category = Category.NONE // ← YENİ
                ) {
        viewModelScope.launch {
            try {
                val newTask = Task(
                    title = title,
                    dueDateTime = dueDateTime,
                    repeatType = repeatType,
                    reminderMinutesBefore = reminderMinutesBefore,
                    category = category // ← YENİ
                )
                repository.insert(newTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), newTask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun updateTaskTitle(task: Task, newTitle: String) {
        if (newTitle.isBlank()) return // Boş başlığa izin verme
        viewModelScope.launch {
            try {
                repository.update(task.copy(title = newTitle.trim()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                ReminderScheduler.cancelReminder(getApplication<Application>(), task)
                repository.delete(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val logs: StateFlow<List<TaskLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun markAsCompleted(task: Task) {
        viewModelScope.launch {
            try {
                if (task.repeatType == RepeatType.NONE) {
                    // Tek seferlik görev: Tamamlandı yap, alarmı iptal et
                    repository.update(task.copy(status = TaskStatus.COMPLETED))
                    ReminderScheduler.cancelReminder(getApplication<Application>(), task)
                    repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.COMPLETED))
                } else {
                    // Tekrarlayan görev: Bir sonraki tarihi hesapla
                    val nextDate = ReminderScheduler.getNextOccurrence(task.dueDateTime, task.repeatType)
                    if (nextDate != null) {
                        val updatedTask = task.copy(
                            dueDateTime = nextDate,
                            status = TaskStatus.PENDING // Yeni tarih için tekrar PENDING
                        )
                        repository.update(updatedTask)
                        // Yeni tarih için alarm planla
                        ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
                        repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.COMPLETED))
                    }
                }
            } catch (e: Exception) { e.printStackTrace()}
        }
    }

    fun markAsMissed(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task.copy(status = TaskStatus.MISSED))
                ReminderScheduler.cancelReminder(getApplication<Application>(), task)
                repository.insertLog(TaskLog(taskTitle = task.title, status = TaskStatus.MISSED))
            } catch (e: Exception) { e.printStackTrace()}
        }
    }

    fun markAsPending(task: Task, newDateTime: LocalDateTime) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(
                    status = TaskStatus.PENDING,
                    dueDateTime = newDateTime
                )
                repository.update(updatedTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTaskDate(task: Task, newDate: java.time.LocalDate) {
        viewModelScope.launch {
            try {
                val newDateTime = task.dueDateTime
                    .withYear(newDate.year)
                    .withMonth(newDate.monthValue)
                    .withDayOfMonth(newDate.dayOfMonth)
                val updatedTask = task.copy(
                    dueDateTime = newDateTime,
                    status = TaskStatus.PENDING
                )
                repository.update(updatedTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTaskTime(task: Task, newHour: Int, newMinute: Int) {
        viewModelScope.launch {
            try {
                val newDateTime = task.dueDateTime
                    .withHour(newHour)
                    .withMinute(newMinute)
                val updatedTask = task.copy(
                    dueDateTime = newDateTime,
                    status = TaskStatus.PENDING
                )
                repository.update(updatedTask)
                ReminderScheduler.scheduleReminder(getApplication<Application>(), updatedTask)
            } catch (e: Exception) { e.printStackTrace()}
        }
    }
}