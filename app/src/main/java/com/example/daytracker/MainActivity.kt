package com.example.daytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.daytracker.data.AppDatabase
import com.example.daytracker.data.TaskRepository
import com.example.daytracker.ui.HomeScreen
import com.example.daytracker.ui.TaskViewModel
import com.example.daytracker.ui.TaskViewModelFactory
import com.example.daytracker.ui.StatisticsViewModel
import com.example.daytracker.ui.StatisticsViewModelFactory
import com.example.daytracker.ui.theme.DayTrackerTheme
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModelFactory = TaskViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[TaskViewModel::class.java]
        
        val statsViewModelFactory = StatisticsViewModelFactory(repository)
        val statsViewModel = ViewModelProvider(this, statsViewModelFactory)[StatisticsViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { /* Permission handled */ }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            DayTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Scaffold(
                        bottomBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), 
                                        thickness = 1.dp
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val items = listOf(
                                            Triple("home", "Home", Icons.Default.Home),
                                            Triple("stats", "Stats", Icons.Default.Assessment)
                                        )
                                        
                                        items.forEach { (route, label, icon) ->
                                            val isSelected = currentRoute == route
                                            val contentColor by animateColorAsState(
                                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                animationSpec = tween(300), 
                                                label = "navColor"
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = androidx.compose.material3.ripple(bounded = false, radius = 40.dp),
                                                        onClick = {
                                                            navController.navigate(route) {
                                                                popUpTo(navController.graph.startDestinationId) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = label,
                                                        tint = contentColor,
                                                        modifier = Modifier.size(28.dp) // Slightly increased size
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = if(isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium
                                                        ),
                                                        color = contentColor
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    
                                                    val lineWidth by animateFloatAsState(
                                                        targetValue = if (isSelected) 1f else 0f, 
                                                        animationSpec = tween(300),
                                                        label = "lineWidth"
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .width(24.dp * lineWidth)
                                                            .height(3.dp)
                                                            .clip(RoundedCornerShape(50))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") { HomeScreen(viewModel = viewModel) }
                            composable("stats") { com.example.daytracker.ui.StatisticsScreen(viewModel = statsViewModel) }
                        }
                    }
                }
            }
        }
    }
}