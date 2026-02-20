package com.example.daytracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.daytracker.data.Priority
import com.example.daytracker.data.Task
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    selectedDateInMillis: Long,
    initialTask: Task? = null,
    onTaskSaved: (Task) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: Priority.MEDIUM) }
    var hasReminder by remember { mutableStateOf(initialTask?.hasReminder ?: false) }
    
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var selectedTime by remember { 
        mutableStateOf(
            if (initialTask?.timeInMillis != null) {
                Calendar.getInstance().apply { timeInMillis = initialTask.timeInMillis }
            } else null
        ) 
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp), // Extra padding for system nav
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialTask != null) "Edit Task" else "Add New Task",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Priority Selection
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name) }
                    )
                }
            }

            // Deadline / Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Deadline Time", style = MaterialTheme.typography.bodyLarge)
                    if (selectedTime != null) {
                        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                        Text(
                            text = format.format(selectedTime!!.time),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(if (selectedTime == null) "Select Time" else "Change Time")
                }
            }

            // Reminder Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set Reminder", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = hasReminder,
                    onCheckedChange = { hasReminder = it },
                    enabled = selectedTime != null // Only enable if a time is selected
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalTimeInMillis = if (selectedTime != null) {
                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateInMillis }
                            cal.set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.timeInMillis
                        } else null

                        val task = Task(
                            title = title,
                            description = description,
                            priority = priority,
                            dateInMillis = selectedDateInMillis,
                            hasReminder = hasReminder && finalTimeInMillis != null,
                            timeInMillis = finalTimeInMillis
                        )
                        onTaskSaved(task)
                        onDismissRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("Save Task")
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                cal.set(Calendar.MINUTE, timePickerState.minute)
                selectedTime = cal
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
