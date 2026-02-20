package com.example.daytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.daytracker.data.AppDatabase
import com.example.daytracker.data.TaskRepository
import com.example.daytracker.ui.HomeScreen
import com.example.daytracker.ui.TaskViewModel
import com.example.daytracker.ui.TaskViewModelFactory
import com.example.daytracker.ui.theme.DayTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModelFactory = TaskViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[TaskViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            DayTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}