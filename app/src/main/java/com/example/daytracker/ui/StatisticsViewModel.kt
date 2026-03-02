package com.example.daytracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.daytracker.data.Task
import com.example.daytracker.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar

class StatisticsViewModel(private val repository: TaskRepository) : ViewModel() {

    private val allTasks = repository.getAllTasks()
    
    // State flow for the currently selected month. Defaults to the current month.
    val selectedMonth = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    })
    
    fun nextMonth() {
        selectedMonth.update { cal ->
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        }
    }
    
    fun previousMonth() {
        selectedMonth.update { cal ->
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        }
    }

    val totalTasksCompletedInMonth: StateFlow<Int> = combine(allTasks, selectedMonth) { tasks, monthCal ->
        val (startOfMonth, endOfMonth) = getMonthRange(monthCal)
        tasks.count { it.isCompleted && it.dateInMillis in startOfMonth..endOfMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyCompletionRate: StateFlow<Float> = combine(allTasks, selectedMonth) { tasks, monthCal ->
        val (startOfMonth, endOfMonth) = getMonthRange(monthCal)
        val tasksThisMonth = tasks.filter { it.dateInMillis in startOfMonth..endOfMonth }
        if (tasksThisMonth.isEmpty()) return@combine 0f
        val completed = tasksThisMonth.count { it.isCompleted }
        completed.toFloat() / tasksThisMonth.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Represents weeks 1 to 5 (or 6 depending on the month). Limits to 5 for simplicity in UI.
    val tasksCompletedPerWeekInMonth: StateFlow<List<Int>> = combine(allTasks, selectedMonth) { tasks, monthCal ->
        val (startOfMonth, endOfMonth) = getMonthRange(monthCal)
        val tasksThisMonth = tasks.filter { it.isCompleted && it.dateInMillis in startOfMonth..endOfMonth }
        
        val maxWeek = monthCal.getActualMaximum(Calendar.WEEK_OF_MONTH)
        val counts = IntArray(maxWeek) { 0 }
        
        tasksThisMonth.forEach { task ->
            val cal = Calendar.getInstance().apply { timeInMillis = task.dateInMillis }
            val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)
            // weekOfMonth is 1-indexed, array is 0-indexed
            val index = weekOfMonth - 1
            if (index in counts.indices) {
                counts[index]++
            }
        }
        counts.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostProductiveDay: StateFlow<Long?> = combine(allTasks, selectedMonth) { tasks, monthCal ->
        val (startOfMonth, endOfMonth) = getMonthRange(monthCal)
        val completedTasks = tasks.filter { it.isCompleted && it.dateInMillis in startOfMonth..endOfMonth }
        if (completedTasks.isEmpty()) return@combine null
        
        // Group by the start of the day dateInMillis
        val groupedByDay = completedTasks.groupBy { task ->
            val cal = Calendar.getInstance().apply { timeInMillis = task.dateInMillis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        
        val maxEntry = groupedByDay.maxByOrNull { it.value.size }
        maxEntry?.key
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun getMonthRange(monthCal: Calendar): Pair<Long, Long> {
        val cal = monthCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}

class StatisticsViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
