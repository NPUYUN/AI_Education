package com.example.ai_tutor.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
// ... other imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai_tutor.presentation.ChatScreen

import com.example.ai_tutor.presentation.AiTutorViewModel

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: AiTutorViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF8F8F8)) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    "home" to Icons.Default.Home,
                    "geometry" to Icons.Default.Edit,
                    "timeline" to Icons.Default.Timeline,
                    "video" to Icons.Default.VideoLibrary,
                    "profile" to Icons.Default.Person
                )
                
                val labels = listOf("AI 辅导", "几何解题", "时间轴地图", "视频总结", "个人主页")

                items.forEachIndexed { index, (route, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(labels[index]) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { ChatScreen(modifier = Modifier.fillMaxSize()) } // Padding already handled by NavHost? No, wait.
            // If innerPadding is applied to NavHost, ChatScreen is inside the padded area.
            // So ChatScreen should not need extra padding, BUT if ChatScreen Scaffold expands, it fits inside.
            // Let's verify. NavHost(modifier = Modifier.padding(innerPadding))
            // So content is drawn above BottomBar.
            // If BottomBar is missing, maybe ChatScreen background is white and opaque?
            // ChatScreen has Scaffold(containerColor = Color.White).
            // It should be fine.
            // Maybe MainScreen Scaffold bottomBar is not showing?
            // I'll leave it as is, since NavHost has padding.
            // Wait, I should pass modifier to ChatScreen just in case.
            // Actually, composable("home") { ChatScreen() } is standard.
            // But I updated ChatScreen to accept modifier.
            // If I don't pass anything, it uses Modifier.
            // Let's pass fillMaxSize() {
            composable("home") { 
                ChatScreen(
                    viewModel = viewModel,
                    onCameraClick = onNavigateToCamera,
                    modifier = Modifier.fillMaxSize()
                ) 
            }
            composable("geometry") { GeometryScreen() }
            composable("timeline") { TimelineScreen() }
            composable("video") { VideoSummaryScreen() }
            composable("profile") { ProfileScreen(onNavigateToSettings, onLogout) }
        }
    }
}
