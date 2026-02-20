package com.example.daytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daytracker.data.Task
import com.example.daytracker.ui.components.AddTaskSheet
import com.example.daytracker.ui.components.TaskItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TaskViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day Tracker", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple Calendar Strip (Week view or Month view placeholder)
            // For a full month calendar, a library like `kizitonwose/Calendar` is usually used, 
            // but we'll build a basic compact week view for simplicity and built-in compose components.
            ExpandableCalendar(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Tasks for ${formatDate(selectedDate)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks for this day.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                            onEditClick = { 
                                taskToEdit = task
                                showAddTaskSheet = true 
                            },
                            onDeleteClick = { viewModel.deleteTask(task, context) }
                        )
                    }
                }
            }
        }

        if (showAddTaskSheet) {
            AddTaskSheet(
                selectedDateInMillis = selectedDate,
                initialTask = taskToEdit,
                onTaskSaved = { task ->
                    if (taskToEdit != null) {
                        viewModel.updateTask(task.copy(id = taskToEdit!!.id), context)
                    } else {
                        viewModel.addTask(task, context)
                    }
                },
                onDismissRequest = { 
                    showAddTaskSheet = false 
                    taskToEdit = null
                }
            )
        }
    }
}

@Composable
fun ExpandableCalendar(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var currentVisibleDate by remember(selectedDate, isExpanded) { mutableLongStateOf(selectedDate) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            Text(
                text = monthFormat.format(Date(currentVisibleDate)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle Calendar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isExpanded) {
            MonthView(selectedDate, onDateSelected) { newVisibleDate ->
                currentVisibleDate = newVisibleDate
            }
        } else {
            WeekView(selectedDate, onDateSelected)
        }
    }
}

@Composable
fun WeekView(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val dates = remember(selectedDate) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val weekDates = mutableListOf<Long>()
        for (i in 0..6) {
            weekDates.add(calendar.timeInMillis)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        weekDates
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dates.forEach { date ->
            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toString()
            
            val isSelected = isSameDay(date, selectedDate)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onDateSelected(date) }
                    .padding(8.dp)
            ) {
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dayOfMonth,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MonthView(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onVisibleMonthChanged: (Long) -> Unit
) {
    // Generate dates around the current selectedDate to support infinite scrolling.
    // We use a large page count and start in the middle.
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })
    val currentMonthDate = remember { mutableLongStateOf(selectedDate) }

    // Update the base month when the pager changes
    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - initialPage
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        calendar.add(Calendar.MONTH, monthOffset)
        currentMonthDate.longValue = calendar.timeInMillis
        onVisibleMonthChanged(calendar.timeInMillis)
    }

    // Render the calendar header static outside pager OR let the Pager hold the grids
    HorizontalPager(state = pagerState) { page ->
        val monthOffset = page - initialPage
        val pageCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        pageCalendar.add(Calendar.MONTH, monthOffset)
        val targetMonthDate = pageCalendar.timeInMillis
        
        MonthGrid(
            targetMonthDate = targetMonthDate,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun MonthGrid(
    targetMonthDate: Long,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val daysInMonth = remember(targetMonthDate) {
        val calendar = Calendar.getInstance().apply { timeInMillis = targetMonthDate }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val datesList = mutableListOf<Long?>()
        
        for (i in 1 until firstDayOfWeek) {
            datesList.add(null)
        }
        
        for (i in 1..maxDays) {
            datesList.add(calendar.timeInMillis)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        datesList
    }

    Column {
        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val weeks = daysInMonth.chunked(7)
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..6) {
                    val date = week.getOrNull(i)
                    if (date != null) {
                        val calendar = Calendar.getInstance().apply { timeInMillis = date }
                        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toString()
                        val isSelected = isSameDay(date, selectedDate)
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onDateSelected(date) }
                        ) {
                            Text(
                                text = dayOfMonth,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp))
                    }
                }
            }
        }
    }
}

fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun formatDate(dateInMillis: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(dateInMillis))
}
