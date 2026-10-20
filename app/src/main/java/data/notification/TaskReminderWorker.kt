package com.example.mindme.data.notification

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mindme.data.AppDatabase
import com.example.mindme.data.TaskStatus
import kotlinx.coroutines.flow.first

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val taskId = inputData.getString("TASK_ID")
        Log.d("MindMe", "🔔 ========== WORKER ÇALIŞTI ==========")
        Log.d("MindMe", "📌 Task ID: $taskId")

        if (taskId == null) {
            Log.e("MindMe", "❌ TASK_ID yok!")
            return Result.failure()
        }

        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.taskDao()

            val allTasks = kotlinx.coroutines.runBlocking {
                dao.getAllTasks().first()
            }

            val task = allTasks.find { it.id == taskId }

            if (task == null) {
                Log.d("MindMe", "⏭️ Görev bulunamadı (silinmiş olabilir)")
                return Result.success()
            }

            Log.d("MindMe", "📋 Görev: ${task.title}")
            Log.d("MindMe", "📊 Durum: ${task.status}")

            // Sadece hâlâ beklemede olan görevler için bildirim gönder
            if (task.status == TaskStatus.PENDING) {
                Log.d("MindMe", "📤 Bildirim gönderiliyor: ${task.title}")
                NotificationHelper.showNotification(
                    context = applicationContext,
                    taskId = task.id,
                    title = "⏰ Görev Zamanı!",
                    message = task.title
                )
            } else {
                Log.d("MindMe", "⏭️ Görev zaten tamamlanmış/iptal, bildirim yok")
            }

            Log.d("MindMe", "🔔 ========== WORKER BİTTİ ==========")
            Result.success()
        } catch (e: Exception) {
            Log.e("MindMe", "❌ Worker hatası", e)
            Result.retry()
        }
    }
}