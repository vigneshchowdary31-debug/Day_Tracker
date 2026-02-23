package com.example.daytracker.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksForDate(dateInMillis: Long): Flow<List<Task>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.timeInMillis

        return taskDao.getTasksForDate(startOfDay, endOfDay)
    }

    fun getAllTaskDates(): Flow<List<Long>> {
        return taskDao.getAllTaskDates()
    }

    fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
    }

    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}
