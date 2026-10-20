package com.example.mindme.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }
    suspend fun markOverdueTasksAsMissed() {
        val now = java.time.LocalDateTime.now().toString()
        taskDao.markOverdueAsMissed(now)
    }
}