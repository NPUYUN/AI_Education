package com.example.education.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai_tutor.ui.ChatScreen
import com.example.education.MainViewModel
import com.example.education.ui.auth.LoginScreen
import com.example.education.ui.auth.RegisterScreen
import com.example.education.ui.settings.SettingsScreen
import com.example.geometry_solver.ui.CameraScreen
import com.example.timeline_map.ui.TimelineScreen
import com.example.video_summarizer.ui.SummaryScreen

import androidx.compose.ui.res.stringResource
import com.example.education.R

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector? = null) {
    object Login : Screen("login", R.string.login_title)
    object Register : Screen("register", R.string.register_title)
    object Main : Screen("main", R.string.app_name)
    object Settings : Screen("settings", R.string.settings_title, Icons.Filled.Settings)
    
    // Main Tabs
    object AiTutor : Screen("ai_tutor", R.string.tab_ai_tutor, Icons.Filled.Face)
    object Geometry : Screen("geometry", R.string.tab_geometry, Icons.Filled.Home)
    object Timeline : Screen("timeline", R.string.tab_timeline, Icons.Filled.DateRange)
    object Video : Screen("video", R.string.tab_summary, Icons.Filled.PlayArrow)
}

@Composable
fun EducationApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Handle initial navigation based on login state
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Screen.Main.route) {
                popUpTo(0)
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(0)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                userRepository = viewModel.userRepository,
                onLoginSuccess = { /* Handled by LaunchedEffect */ },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                userRepository = viewModel.userRepository,
                onRegisterSuccess = { 
                    navController.popBackStack() // Go back to Login
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel)
        }
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToSettings: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.AiTutor,
        Screen.Geometry,
        Screen.Timeline,
        Screen.Video
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = stringResource(screen.titleResId)) },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
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
            startDestination = Screen.AiTutor.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.AiTutor.route) { ChatScreen() }
            composable(Screen.Geometry.route) { CameraScreen() }
            composable(Screen.Timeline.route) { TimelineScreen() }
            composable(Screen.Video.route) { SummaryScreen() }
        }
    }
}
