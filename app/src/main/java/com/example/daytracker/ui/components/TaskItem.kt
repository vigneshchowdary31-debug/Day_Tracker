package com.example.daytracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.tween
import com.example.daytracker.data.Priority
import com.example.daytracker.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    isSelected: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onEditClick()
                return@rememberSwipeToDismissBoxState false // Do not dismiss for edit
            }
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDeleteClick()
                return@rememberSwipeToDismissBoxState true // Dismiss for delete
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.animateContentSize(), // For smooth delete animation
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val backgroundColor by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                }, label = "dismissBackground"
            )
            val icon = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }
            val alignment = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val iconTint = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onError
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.scale(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.5f else 1.2f)
                    )
                }
            }
        },
        content = {
            val isOverdue = androidx.compose.runtime.remember(task) {
                if (task.isCompleted) return@remember false
                val now = System.currentTimeMillis()
                if (task.timeInMillis != null) {
                    task.timeInMillis < now
                } else {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    task.dateInMillis < cal.timeInMillis
                }
            }

            val cardColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) // Keep alpha very low or 0.dp elevation to prevent shadow bleed
                    else -> MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(durationMillis = 300),
                label = "task_selection_color"
            )
            
            val elevation by animateFloatAsState(
                targetValue = if (task.isCompleted || isOverdue || isSelected) 0f else 0f,
                label = "task_elevation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onEditClick,
                        onLongClick = onLongClick
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                val priorityColor = when(task.priority) {
                    Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    Priority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    Priority.LOW -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical Colored Bar
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp, top = 6.dp, bottom = 6.dp)
                            .width(8.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(priorityColor)
                    )

                    // Content inside
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val scale by animateFloatAsState(
                            targetValue = if (task.isCompleted) 1.2f else 1f, 
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "checkbox_scale"
                        )
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { 
                                if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCheckedChange(it) 
                            },
                            modifier = Modifier.scale(scale)
                        )
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            
                            if (task.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Priority Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(priorityColor)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = task.priority.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    if (isOverdue) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Overdue",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                
                                // Time text with clock icon
                                    if (task.timeInMillis != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Time",
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                                            Text(
                                                text = format.format(Date(task.timeInMillis)),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }                          }
                        }
                    }
                }
            }
        }
    )
}
