package com.example.daytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import com.example.daytracker.data.Task
import com.example.daytracker.ui.components.AddTaskSheet
import com.example.daytracker.ui.components.TaskItem
import com.example.daytracker.ui.theme.PrimaryGradientEnd
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.ripple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TaskViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val taskDates by viewModel.taskDates.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val listState = rememberLazyListState()
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    var bottomSheetKey by remember { mutableIntStateOf(0) }

    // Scroll to top when filter or sort changes
    LaunchedEffect(filterType, sortType) {
        if (tasks.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = selectedTaskIds.isNotEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "topBarTransition"
            ) { isSelectionMode ->
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedTaskIds.size} Selected", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.markSelectedTasksComplete(tasks) }) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Mark Complete")
                            }
                            IconButton(onClick = { viewModel.deleteSelectedTasks(context, tasks) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Day Tracker",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            Box {
                                IconButton(onClick = { showFilterMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filter Tasks"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Tasks") },
                                        onClick = {
                                            viewModel.setFilterType(null)
                                            showFilterMenu = false
                                        },
                                        trailingIcon = if (filterType == null) {
                                            { Icon(androidx.compose.material.icons.Icons.Default.Check, "Selected") }
                                        } else null
                                    )
                                    HorizontalDivider()
                                    FilterType.entries.forEach { filter ->
                                        DropdownMenuItem(
                                            text = { Text(filter.displayName) },
                                            onClick = {
                                                viewModel.setFilterType(filter)
                                                showFilterMenu = false
                                            },
                                            trailingIcon = if (filterType == filter) {
                                                { Icon(androidx.compose.material.icons.Icons.Default.Check, "Selected") }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            // Smooth bouncy scale on press
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "fabScaleAnimation"
            )

            val isScrolling = listState.isScrollInProgress
            LaunchedEffect(isScrolling) {
                if (isScrolling && isFirstLaunch) {
                    isFirstLaunch = false // Collapse text on first scroll
                }
            }
            
            // Dynamic elevation: rises slightly during scroll
            val fabElevation by animateDpAsState(
                targetValue = if (isPressed) 12.dp else if (isScrolling) 10.dp else 6.dp,
                animationSpec = tween(durationMillis = 200),
                label = "fabElevation"
            )
            
            ExtendedFloatingActionButton(
                onClick = { 
                    if (isFirstLaunch) isFirstLaunch = false
                    bottomSheetKey++
                    showAddTaskSheet = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = fabElevation,
                    pressedElevation = 12.dp,
                ),
                interactionSource = interactionSource,
                expanded = isFirstLaunch, // First-time tooltip behaviour
                icon = {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Add Task",
                        modifier = Modifier.size(28.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add Task",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                modifier = Modifier.scale(scale)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            


            CompactDateSelector(
                selectedDate = selectedDate,
                taskDates = taskDates,
                onDateSelected = { viewModel.selectDate(it) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                
                SegmentedSortControl(
                    selectedType = sortType,
                    onTypeSelected = { viewModel.setSortType(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (tasks.isNotEmpty()) {
                val completedTasks = tasks.count { it.isCompleted }
                val totalTasks = tasks.size
                val targetProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
                var loadProgress by remember { mutableFloatStateOf(0f) }
                
                LaunchedEffect(targetProgress) {
                    loadProgress = targetProgress
                }
                
                val animatedProgress by animateFloatAsState(
                    targetValue = loadProgress,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                    label = "progressAnimation"
                )
                
                val isComplete = completedTasks == totalTasks && totalTasks > 0

                val primaryColor = MaterialTheme.colorScheme.primary
                val gradientEnd = PrimaryGradientEnd
                val successColor = Color(0xFF10B981) // Green
                
                val ringBrush by remember(isComplete, primaryColor, gradientEnd, successColor) {
                    derivedStateOf {
                        if (isComplete) {
                            Brush.sweepGradient(listOf(successColor, successColor))
                        } else {
                            Brush.sweepGradient(listOf(primaryColor, gradientEnd, primaryColor)) // Smooth gradient cycle
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                                drawArc(
                                    brush = ringBrush,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                            
                            androidx.compose.animation.AnimatedContent(targetState = isComplete, label = "ProgressCenter") { complete ->
                                if (complete) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "All done",
                                        tint = successColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Daily Progress – $completedTasks of $totalTasks completed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            AnimatedContent(
                targetState = tasks.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "taskListAnimation"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "All caught up",
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No tasks today \uD83C\uDF89",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enjoy your free time!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                isSelected = selectedTaskIds.contains(task.id),
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                onEditClick = { 
                                    if (selectedTaskIds.isNotEmpty()) {
                                        viewModel.toggleTaskSelection(task.id)
                                    } else {
                                        taskToEdit = task
                                        showAddTaskSheet = true 
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleTaskSelection(task.id)
                                },
                                onDeleteClick = { 
                                    viewModel.deleteTask(task, context)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Task deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addTask(task, context)
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(durationMillis = 250),
                                    fadeOutSpec = tween(durationMillis = 250),
                                    placementSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showAddTaskSheet) {
            key(bottomSheetKey) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDateSelector(
    selectedDate: Long,
    taskDates: Set<Long>,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { showBottomSheet = true },
        shape = RoundedCornerShape(percent = 50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📅", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatDate(selectedDate),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Change Date",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                var currentVisibleDate by remember(selectedDate) { mutableLongStateOf(selectedDate) }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    Text(
                        text = monthFormat.format(Date(currentVisibleDate)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                MonthView(
                    selectedDate = selectedDate, 
                    taskDates = taskDates, 
                    onDateSelected = { 
                        onDateSelected(it)
                        showBottomSheet = false
                    },
                    onVisibleMonthChanged = { newVisibleDate ->
                        currentVisibleDate = newVisibleDate
                    }
                )
            }
        }
    }
}

@Composable
fun MonthView(
    selectedDate: Long,
    taskDates: Set<Long>,
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
            taskDates = taskDates,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun MonthGrid(
    targetMonthDate: Long,
    selectedDate: Long,
    taskDates: Set<Long>,
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..6) {
                    val date = week.getOrNull(i)
                    if (date != null) {
                        val calendar = Calendar.getInstance().apply { timeInMillis = date }
                        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toString()
                        val isSelected = isSameDay(date, selectedDate)

                        // Smooth background and text color transitions
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(durationMillis = 300),
                            label = "DateSelectionAnimation"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            animationSpec = tween(durationMillis = 300),
                            label = "DateTextColorAnimation"
                        )
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .clickable { onDateSelected(date) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayOfMonth,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Dot indicator for tasks
                                if (taskDates.contains(date)) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
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

@Composable
fun SegmentedSortControl(
    selectedType: SortType,
    onTypeSelected: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(SortType.TIME to "Time", SortType.PRIORITY to "Priority")
    
    Surface(
        modifier = modifier.height(28.dp),
        shape = RoundedCornerShape(percent = 50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (type, label) ->
                val isSelected = type == selectedType
                
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "SegmentBackgroundColor"
                )
                
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "SegmentContentColor"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = MaterialTheme.colorScheme.primary),
                            onClick = { onTypeSelected(type) }
                        )
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}


