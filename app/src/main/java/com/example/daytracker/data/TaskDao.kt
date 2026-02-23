package com.example.daytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE dateInMillis >= :startOfDay AND dateInMillis <= :endOfDay ORDER BY priority DESC, timeInMillis ASC")
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    @Query("SELECT dateInMillis FROM tasks")
    fun getAllTaskDates(): Flow<List<Long>>

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?
}
