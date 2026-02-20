package com.example.daytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dateInMillis: Long, // to associate with a specific day
    val timeInMillis: Long? = null, // for the reminder/due time
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false
)

enum class Priority {
    LOW, MEDIUM, HIGH
}
