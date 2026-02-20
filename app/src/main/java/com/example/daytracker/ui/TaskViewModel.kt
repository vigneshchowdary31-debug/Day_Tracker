package com.example.daytracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.daytracker.data.Task
import com.example.daytracker.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import android.content.Context
import com.example.daytracker.util.ReminderManager

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getTodayInMillis())
    val selectedDate = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date ->
            repository.getTasksForDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectDate(dateInMillis: Long) {
        _selectedDate.value = dateInMillis
    }

    fun addTask(task: Task, context: Context) {
        viewModelScope.launch {
            val id = repository.insertTask(task)
            ReminderManager.scheduleReminder(context, task.copy(id = id))
        }
    }

    fun updateTask(task: Task, context: Context) {
        viewModelScope.launch {
            repository.updateTask(task)
            ReminderManager.cancelReminder(context, task)
            ReminderManager.scheduleReminder(context, task)
        }
    }

    fun deleteTask(task: Task, context: Context) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderManager.cancelReminder(context, task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    private fun getTodayInMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
