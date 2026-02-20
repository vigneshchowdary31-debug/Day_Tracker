package com.example.daytracker.util

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.daytracker.data.Task
import com.example.daytracker.worker.ReminderWorker
import java.util.concurrent.TimeUnit

object ReminderManager {

    fun scheduleReminder(context: Context, task: Task) {
        if (!task.hasReminder || task.timeInMillis == null) return

        val workManager = WorkManager.getInstance(context)
        val delay = task.timeInMillis - System.currentTimeMillis()

        if (delay <= 0) return // Time passed already

        val data = Data.Builder()
            .putString("taskTitle", task.title)
            .putLong("taskId", task.id)
            .build()

        val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("task_${task.id}")
            .build()

        workManager.enqueue(reminderWork)
    }

    fun cancelReminder(context: Context, task: Task) {
        // In a real app, track work uuid to cancel by ID instead of tags or just not care if deleted
        // Usually, set tag = "task_${task.id}" in the WorkRequest builder, then cancelAllWorkByTag
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("task_${task.id}")
    }
}
