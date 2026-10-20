package com.example.mindme.data.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mindme.data.AppDatabase
import com.example.mindme.data.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val action = intent.action ?: return

        Log.d("MindMe", "🔔 Bildirim aksiyonu: $action | Task ID: $taskId")

        when (action) {
            "MARK_COMPLETED" -> markTaskCompleted(context, taskId)
        }
    }

    private fun markTaskCompleted(context: Context, taskId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.taskDao()

                val allTasks = dao.getAllTasks().first()
                val task = allTasks.find { it.id == taskId }

                if (task != null) {
                    dao.updateTask(task.copy(status = TaskStatus.COMPLETED))
                    Log.d("MindMe", "✅ Görev tamamlandı: ${task.title}")
                }

                // Bildirimi kapat
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.hashCode())
            } catch (e: Exception) {
                Log.e("MindMe", "❌ Tamamlandı işaretlenemedi", e)
            }
        }
    }
}