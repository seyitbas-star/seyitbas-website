package com.example.mindme.data.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mindme.data.Task
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val TAG = "MindMe"
    private const val MINUTES_BEFORE = 3L

    fun scheduleReminder(context: Context, task: Task) {
        try {
            val triggerTime = task.dueDateTime.minusMinutes(MINUTES_BEFORE)
            val now = LocalDateTime.now()

            // Görev zamanı geçmişse planlama yapma
            if (triggerTime.isBefore(now)) {
                Log.d(TAG, "⏭️ Alarm planlanmadı (geçmiş): ${task.title}")
                return
            }

            val delaySeconds = Duration.between(now, triggerTime).seconds

            if (delaySeconds <= 0) {
                Log.d(TAG, "⏭️ Gecikme sıfır/negatif, planlanmadı: ${task.title}")
                return
            }

            Log.d(TAG, "⏰ Alarm planlanıyor: ${task.title} - ${delaySeconds} saniye sonra")

            // Görev ID'sini input data olarak gönder
            val inputData = Data.Builder()
                .putString("TASK_ID", task.id)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build()

            // Her görev için benzersiz iş adı: "task_<uuid>"
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "task_${task.id}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Alarm planlama hatası: ${task.title}", e)
        }
    }

    fun cancelReminder(context: Context, task: Task) {
        try {
            Log.d(TAG, "❌ Alarm iptal ediliyor: ${task.title}")
            WorkManager.getInstance(context)
                .cancelUniqueWork("task_${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Alarm iptal hatası: ${task.title}", e)
        }
    }
}