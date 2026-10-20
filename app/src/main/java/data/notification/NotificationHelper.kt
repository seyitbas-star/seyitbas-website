package com.example.mindme.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mindme.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "mindme_reminders"
    private const val CHANNEL_NAME = "Görev Hatırlatıcıları"
    private const val CHANNEL_DESC = "Zamanı gelen görevler için hatırlatıcılar"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, taskId: String, title: String, message: String) {
        // 1. Bildirime tıklayınca uygulamayı aç
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. "Tamamlandı" butonu için BroadcastReceiver intent
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "MARK_COMPLETED"
            putExtra("TASK_ID", taskId)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Bildirimi oluştur
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✅ Tamamlandı",
                completePendingIntent
            )

        try {
            NotificationManagerCompat.from(context)
                .notify(taskId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}