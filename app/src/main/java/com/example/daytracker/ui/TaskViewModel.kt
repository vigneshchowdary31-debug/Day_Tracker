package com.example.daytracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.daytracker.data.Task
import com.example.daytracker.data.TaskRepository
import com.example.daytracker.data.Priority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import android.content.Context
import com.example.daytracker.util.ReminderManager

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getTodayInMillis())
    val selectedDate = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.TODAY)
    val viewMode = _viewMode.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.TIME)
    val sortType = _sortType.asStateFlow()

    private val _filterType = MutableStateFlow<FilterType?>(null)
    val filterType = _filterType.asStateFlow()

    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTaskIds = _selectedTaskIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: StateFlow<List<Task>> = combine(
        combine(_selectedDate, _viewMode) { date, mode -> Pair(date, mode) }
            .flatMapLatest { (date, mode) ->
                if (mode == ViewMode.TODAY) {
                    repository.getTasksForDate(date)
                } else {
                    val (start, end) = getWeekRange(date)
                    repository.getAllTasks().map { tasks -> tasks.filter { it.dateInMillis in start..end } }
                }
            },
        _sortType,
        _filterType
    ) { tasks, sortType, filterType ->
        val filteredTasks = when (filterType) {
            FilterType.HIGH_PRIORITY -> tasks.filter { it.priority == Priority.HIGH }
            FilterType.INCOMPLETE -> tasks.filter { !it.isCompleted }
            FilterType.WITH_REMINDER -> tasks.filter { it.hasReminder }
            null -> tasks
        }
        
        filteredTasks.sortedWith(
            compareBy<Task> { it.isCompleted } // Incomplete (false) comes before Complete (true)
                .thenBy { 
                    when (sortType) {
                        SortType.TIME -> it.timeInMillis ?: Long.MAX_VALUE // Null time to bottom
                        SortType.PRIORITY -> -it.priority.ordinal // High (2) -> Low (0), so use negative
                    }
                }
                .thenBy { it.id } // Stable fallback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val taskDates: StateFlow<Set<Long>> = repository.getAllTaskDates()
        .map { dates ->
            dates.map { dateInMillis ->
                Calendar.getInstance().apply {
                    timeInMillis = dateInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    fun selectDate(dateInMillis: Long) {
        _selectedDate.value = dateInMillis
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun setFilterType(type: FilterType?) {
        _filterType.value = type
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

    fun toggleTaskSelection(taskId: Long) {
        val current = _selectedTaskIds.value
        _selectedTaskIds.value = if (current.contains(taskId)) {
            current - taskId
        } else {
            current + taskId
        }
    }

    fun clearSelection() {
        _selectedTaskIds.value = emptySet()
    }

    fun deleteSelectedTasks(context: Context, tasks: List<Task>) {
        val selectedIds = _selectedTaskIds.value
        val tasksToDelete = tasks.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            tasksToDelete.forEach { task ->
                repository.deleteTask(task)
                ReminderManager.cancelReminder(context, task)
            }
            clearSelection()
        }
    }

    fun markSelectedTasksComplete(tasks: List<Task>) {
        val selectedIds = _selectedTaskIds.value
        val tasksToComplete = tasks.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            tasksToComplete.forEach { task ->
                repository.updateTask(task.copy(isCompleted = true))
            }
            clearSelection()
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

    private fun getWeekRange(dateInMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateInMillis }
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
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

enum class ViewMode {
    TODAY, WEEK
}

enum class SortType {
    TIME, PRIORITY
}

enum class FilterType(val displayName: String) {
    HIGH_PRIORITY("Only High Priority"),
    INCOMPLETE("Only Incomplete"),
    WITH_REMINDER("Only With Reminder")
}
