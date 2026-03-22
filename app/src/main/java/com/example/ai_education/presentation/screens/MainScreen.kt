package com.example.ai_education.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.summarizer.presentation.VideoDownloadViewModel
import com.example.summarizer.presentation.screens.SummaryMenuScreen
import com.example.ai_tutor.presentation.AiTutorViewModel
import com.example.ai_tutor.presentation.ChatScreen
import com.example.review.presentation.ReviewScreen
import com.example.solver.presentation.SolverScreen
import kotlinx.coroutines.launch

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: AiTutorViewModel,
    videoViewModel: VideoDownloadViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    
    // Bottom Navigation Items
    // Order: AI Tutor, Solver, Summary, Review
    val items = listOf(
        Triple("home", Icons.Default.Face, "AI辅导"),
        Triple("solver", Icons.Default.Edit, "解题"),
        Triple("summary", Icons.Default.Summarize, "总结"),
        Triple("review", Icons.Default.RateReview, "复习")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp) // Fixed width for drawer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                android.widget.Toast.makeText(context, "搜索功能即将上线", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("搜索", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    viewModel.startNewChat()
                                    scope.launch { drawerState.close() }
                                    navController.navigate("home")
                                })
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // User Profile (Top)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                navController.navigate("profile")
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "用户昵称", 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Chat History List
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(sessions) { session ->
                            Text(
                                text = if (session.title.isEmpty()) "新对话" else session.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        viewModel.loadSession(session.id)
                                        scope.launch { drawerState.close() }
                                        navController.navigate("home")
                                    }
                            )
                        }

                        // Static items at the end of the list
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        item {
                            Text(
                                text = "设置",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        onNavigateToSettings()
                                    }
                            )
                        }
                        
                        item {
                            Text(
                                text = "通知",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        android.widget.Toast.makeText(context, "暂无新通知", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                        
                        item {
                            Text(
                                text = "退出登录",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        onLogout()
                                    }
                            )
                        }
                    }

                    // Bottom Section (Footer)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                Icons.Outlined.Notifications, 
                                contentDescription = "Notifications", 
                                modifier = Modifier.clickable { 
                                    android.widget.Toast.makeText(context, "暂无新通知", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            Icon(
                                Icons.Outlined.Settings, 
                                contentDescription = "Settings", 
                                modifier = Modifier.clickable { 
                                    scope.launch { drawerState.close() }
                                    onNavigateToSettings() 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Show TopBar only on Home (AI Tutor) or generally?
                // User asked for "AI Tutor's top left three-bar menu".
                // We'll show it on the Home screen.
                // Or we can just put it on the Scaffold for all main tabs.
                // Let's put it for all main tabs for consistency, or dynamically.
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                // Show top bar on main tabs and profile page
                if ((items.any { it.first == currentRoute } && currentRoute != "timeline" && currentRoute != "video") || currentRoute == "profile") {
                     CenterAlignedTopAppBar(
                        title = { 
                            val label = if (currentRoute == "profile") "个人主页" else items.find { it.first == currentRoute }?.third ?: "AI Tutor"
                            Text(label)
                        },
                        navigationIcon = {
                            if (items.any { it.first == currentRoute } || currentRoute == "profile") {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            } else {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            if (currentRoute == "home") {
                                IconButton(onClick = {
                                    val latest = viewModel.uiState.value.messages.lastOrNull { it.role == "assistant" }
                                    val shareText = when (val c = latest?.content) {
                                        is String -> c
                                        is List<*> -> {
                                            c.filterIsInstance<com.example.common.network.llm.ContentItem>()
                                                .firstOrNull { it.type == "text" }?.text ?: ""
                                        }
                                        else -> ""
                                    }
                                    if (shareText.isNotBlank()) {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        val chooser = android.content.Intent.createChooser(sendIntent, "分享对话内容")
                                        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        try {
                                            context.startActivity(chooser)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (items.any { it.first == currentRoute }) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        val currentDestination = navBackStackEntry?.destination

                        items.forEach { (route, icon, label) ->
                            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                            val scale by animateFloatAsState(if (selected) 1.5f else 1.0f, label = "scale")
                            
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        icon, 
                                        contentDescription = label,
                                        modifier = Modifier.scale(scale)
                                    ) 
                                },
                                label = { 
                                    Text(label, fontSize = 10.sp)
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent, 
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home", // AI Tutor is middle, but usually start destination.
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    ChatScreen(
                        viewModel = viewModel,
                        onCameraClick = onNavigateToCamera,
                        onNavigateToTimeline = { query ->
                            navController.navigate("timeline?query=${android.net.Uri.encode(query)}")
                        },
                        modifier = Modifier.fillMaxSize()
                    ) 
                }
                composable("solver") { SolverScreen() }
                composable("summary") { 
                    SummaryMenuScreen(
                        onNavigateToVideoSummary = { navController.navigate("video") },
                        onNavigateToTextSummary = { android.widget.Toast.makeText(context, "文本总结即将上线", android.widget.Toast.LENGTH_SHORT).show() },
                        onNavigateToChatSummary = { android.widget.Toast.makeText(context, "对话总结即将上线", android.widget.Toast.LENGTH_SHORT).show() }
                    ) 
                }
                composable("review") { ReviewScreen() }
                composable(
                    route = "timeline?query={query}",
                    arguments = listOf(androidx.navigation.navArgument("query") { 
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                    })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query")
                    TimelineScreen(navController, query)
                }
                composable("video") { VideoSummaryScreen(videoViewModel, navController) }
                composable("profile") { ProfileScreen() }
            }
        }
    }
}

