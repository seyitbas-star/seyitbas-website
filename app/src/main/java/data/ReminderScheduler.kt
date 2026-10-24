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
import com.example.mindme.data.RepeatType

object ReminderScheduler {

    private const val TAG = "MindMe"


    fun scheduleReminder(context: Context, task: Task) {
        try {
            // Kullanıcının seçtiği dakikayı al (Varsayılan 2 ise onu kullan)
            val minutesBefore = task.reminderMinutesBefore.toLong()

            // Eğer "Görev saatinde" (0) seçildiyse, tam saatinde uyarsın
            val triggerTime = task.dueDateTime.minusMinutes(minutesBefore)
            val now = LocalDateTime.now()

            if (triggerTime.isBefore(now)) {
                Log.d(TAG, "⏭️ Alarm planlanmadı (geçmiş): ${task.title}")
                return
            }

            val delaySeconds = Duration.between(now, triggerTime).seconds

            if (delaySeconds <= 0) {
                Log.d(TAG, "⏭️ Gecikme sıfır/negatif, planlanmadı: ${task.title}")
                return
            }

            Log.d(TAG, "⏰ Alarm planlanıyor: ${task.title} - ${delaySeconds} saniye sonra ($minutesBefore dk önce)")

            val inputData = Data.Builder()
                .putString("TASK_ID", task.id)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build()

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
    fun getNextOccurrence(dateTime: LocalDateTime, repeatType: RepeatType): LocalDateTime? {
        return when (repeatType) {
            RepeatType.NONE -> null
            RepeatType.DAILY -> dateTime.plusDays(1)
            RepeatType.WEEKLY -> dateTime.plusWeeks(1)
            RepeatType.MONTHLY -> dateTime.plusMonths(1)
            RepeatType.YEARLY -> dateTime.plusYears(1)
        }
    }
}