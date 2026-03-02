package com.example.daytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.daytracker.ui.theme.PrimaryGradientEnd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val totalTasksCompletedInMonth by viewModel.totalTasksCompletedInMonth.collectAsState()
    val monthlyCompletionRate by viewModel.monthlyCompletionRate.collectAsState()
    val mostProductiveDay by viewModel.mostProductiveDay.collectAsState()
    val tasksCompletedPerWeekInMonth by viewModel.tasksCompletedPerWeekInMonth.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)) },
                actions = {
                    MonthPicker(
                        selectedMonth = selectedMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() }
                    )
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp) // Increased spacing
        ) {
            MonthlyDonutChartCard(monthlyCompletionRate) // Moved Top to make it visually prominent

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = "Tasks Done",
                    value = totalTasksCompletedInMonth.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary, // Blue tint background
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), 
                    modifier = Modifier.weight(1f)
                )
                
                val bestDayText = mostProductiveDay?.let {
                    val format = SimpleDateFormat("MMM dd", Locale.getDefault())
                    format.format(Date(it))
                } ?: "N/A"
                
                StatCard(
                    title = "Best Day",
                    value = bestDayText,
                    icon = Icons.Default.Star,
                    color = Color(0xFF10B981), // Green tint explicitly for "productivity"
                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                )
            }

            MonthlyActivityChartCard(tasksCompletedPerWeekInMonth)
        }
    }
}

@Composable
fun MonthPicker(
    selectedMonth: java.util.Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val monthText = monthFormat.format(selectedMonth.time)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = monthText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun MonthlyActivityChartCard(monthlyData: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Activity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            val maxTasks = monthlyData.maxOrNull()?.coerceAtLeast(1) ?: 1
            // Generate labels like W1, W2, etc., based on data size (usually 4-6 weeks)
            val weeks = List(monthlyData.size.coerceAtLeast(4)) { "W${it + 1}" }
            val primaryColor = MaterialTheme.colorScheme.primary
            val gradientEnd = PrimaryGradientEnd
            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            
            var loadProgress by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(Unit) { loadProgress = 1f }
            
            val animationProgress by animateFloatAsState(
                targetValue = loadProgress,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "barAnimation"
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 28.dp)) {
                    val barWidth = 16.dp.toPx()
                    val itemSize = if (monthlyData.isEmpty()) weeks.size else monthlyData.size.coerceAtLeast(weeks.size)
                    val spacing = (size.width - (barWidth * itemSize)) / (itemSize - 1).coerceAtLeast(1)
                    
                    val barBrush = Brush.verticalGradient(listOf(primaryColor, gradientEnd))
                    
                    for (i in 0 until itemSize) {
                        val dataPoint = monthlyData.getOrNull(i) ?: 0
                        val x = i * (barWidth + spacing)
                        val targetHeight = (dataPoint.toFloat() / maxTasks) * size.height
                        val animatedHeight = targetHeight * animationProgress
                        val y = size.height - animatedHeight
                        
                        // Background track
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                        )
                        
                        // Fill bar
                        if (dataPoint > 0) {
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, animatedHeight),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }
                    }
                }
                
                // Labels below canvas
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weeks.forEach { week ->
                        Text(
                            text = week,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyDonutChartCard(completionRate: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "Monthly Rate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                val percentage = (completionRate * 100).toInt()
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "of tasks completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            var loadProgress by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(completionRate) { loadProgress = completionRate }
            
            val animationProgress by animateFloatAsState(
                targetValue = loadProgress,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                label = "donutAnimation"
            )
            
            val primaryColor = MaterialTheme.colorScheme.primary
            val gradientEnd = PrimaryGradientEnd
            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(0.8f).padding(8.dp).aspectRatio(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    drawArc(
                        brush = Brush.sweepGradient(listOf(primaryColor, gradientEnd, primaryColor)),
                        startAngle = -90f,
                        sweepAngle = animationProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
