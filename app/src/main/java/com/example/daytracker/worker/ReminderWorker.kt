package com.example.daytracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.daytracker.MainActivity
import com.example.daytracker.data.AppDatabase

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1)
        
        // Double check database to ensure task is not completed or deleted before firing
        if (taskId != -1L) {
            val db = AppDatabase.getDatabase(context)
            val task = db.taskDao().getTaskById(taskId)
            if (task == null || task.isCompleted) {
                return Result.success()
            }
        }

        val taskTitle = inputData.getString("taskTitle") ?: "Task Reminder"
        showNotification(taskTitle, taskId)

        return Result.success()
    }

    private fun showNotification(title: String, taskId: Long) {
        val channelId = "day_tracker_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // Use standard Android notification icon as placeholder since project might not have specific drawable yet
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Day Tracker Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(taskId.toInt(), builder.build())
    }
}
