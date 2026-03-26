package com.example.ai_education.presentation.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ai_tutor.multimodal_chat.presentation.screens.ChatScreen
import com.example.ai_tutor.multimodal_chat.presentation.viewmodels.AiTutorViewModel
import com.example.review.planner.presentation.screens.ReviewScreen
import com.example.solver.comprehensive.presentation.screens.SolverScreen
import com.example.summarizer.audio_summarizer.presentation.viewmodels.AudioSummaryViewModel
import com.example.summarizer.core.presentation.screens.SummaryMenuScreen
import com.example.summarizer.dialogue_summarizer.presentation.screens.DialogueSummaryScreen
import com.example.summarizer.dialogue_summarizer.presentation.viewmodels.DialogueSummaryViewModel
import com.example.summarizer.knowledge_cards.presentation.screens.KnowledgeCardScreen
import com.example.summarizer.knowledge_cards.presentation.viewmodels.KnowledgeCardViewModel
import com.example.summarizer.text_summarizer.presentation.viewmodels.TextSummaryViewModel
import com.example.summarizer.videosummarizer.presentation.viewmodels.VideoDownloadViewModel
import kotlinx.coroutines.launch
import java.io.File

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToCamera: (String) -> Unit,
    viewModel: AiTutorViewModel,
    videoViewModel: VideoDownloadViewModel = hiltViewModel(),
    outerSavedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember { com.example.common.database.PreferencesManager(context) }
    val savedNickname by preferencesManager.getString("user_nickname", "用户昵称").collectAsState(initial = "用户昵称")
    val savedAvatar by preferencesManager.getString("user_avatar", "").collectAsState(initial = "")

    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    val textSummaryViewModel: TextSummaryViewModel = hiltViewModel()
    val audioSummaryViewModel: AudioSummaryViewModel = hiltViewModel()

    // Bottom Navigation Items
    // Order: AI Tutor, Solver, Summary, Review
    val items =
        listOf(
            Triple("home", Icons.Default.Face, "AI辅导"),
            Triple("solver", Icons.Default.Edit, "解题"),
            Triple("summary", Icons.Default.Summarize, "总结"),
            Triple("review", Icons.Default.RateReview, "复习"),
        )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp), // Fixed width for drawer
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                ) {
                    // Search Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {
                                    android.widget.Toast.makeText(context, "搜索功能即将上线", android.widget.Toast.LENGTH_SHORT).show()
                                },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("搜索", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .clickable {
                                            viewModel.startNewChat()
                                            scope.launch { drawerState.close() }
                                            navController.navigate("home")
                                        },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // User Profile (Top)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("profile")
                                },
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (savedAvatar.isNotEmpty() && File(savedAvatar).exists()) {
                                    AsyncImage(
                                        model =
                                            coil.request.ImageRequest.Builder(context)
                                                .data(File(savedAvatar))
                                                .crossfade(true)
                                                .transformations(coil.transform.CircleCropTransformation())
                                                .build(),
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = savedNickname.ifBlank { "用户昵称" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Chat History List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                    ) {
                        items(sessions) { session ->
                            Text(
                                text = if (session.title.isEmpty()) "新对话" else session.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable {
                                            viewModel.loadSession(session.id)
                                            scope.launch { drawerState.close() }
                                            navController.navigate("home")
                                        },
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
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            onNavigateToSettings()
                                        },
                            )
                        }

                        item {
                            Text(
                                text = "通知",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            android.widget.Toast.makeText(context, "暂无新通知", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                            )
                        }

                        item {
                            Text(
                                text = "退出登录",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            onLogout()
                                        },
                            )
                        }
                    }

                    // Bottom Section (Footer)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "通知",
                                modifier =
                                    Modifier.clickable {
                                        android.widget.Toast.makeText(context, "暂无新通知", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                            )
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "设置",
                                modifier =
                                    Modifier.clickable {
                                        scope.launch { drawerState.close() }
                                        onNavigateToSettings()
                                    },
                            )
                        }
                    }
                }
            }
        },
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
                            if (items.any { it.first == currentRoute }) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "打开菜单")
                                }
                            } else {
                                IconButton(onClick = {
                                    if (currentRoute == "profile") {
                                        // Specific requirement: return to AI tutor
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else {
                                        navController.popBackStack()
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            }
                        },
                        actions = {
                            if (currentRoute == "home") {
                                IconButton(onClick = {
                                    val latest = viewModel.uiState.value.messages.lastOrNull { it.role == "assistant" }
                                    val shareText =
                                        when (val c = latest?.content) {
                                            is String -> c
                                            is List<*> -> {
                                                c.filterIsInstance<com.example.common.network.llm.ContentItem>()
                                                    .firstOrNull { it.type == "text" }?.text ?: ""
                                            }
                                            else -> ""
                                        }
                                    if (shareText.isNotBlank()) {
                                        val sendIntent =
                                            android.content.Intent().apply {
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
                                    Icon(Icons.Default.Share, contentDescription = "分享")
                                }
                            } else if (currentRoute == "profile") {
                                IconButton(onClick = {
                                    onNavigateToSettings()
                                }) {
                                    Icon(Icons.Outlined.Settings, contentDescription = "设置")
                                }
                            }
                        },
                    )
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (items.any { it.first == currentRoute }) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        modifier = Modifier.scale(scale),
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
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home", // AI Tutor is middle, but usually start destination.
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding(),
                enterTransition = {
                    if (initialState.destination.route in items.map { it.first } && targetState.destination.route in items.map { it.first }) {
                        fadeIn(animationSpec = tween(300))
                    } else {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300),
                        ) + fadeIn(animationSpec = tween(300))
                    }
                },
                exitTransition = {
                    if (initialState.destination.route in items.map { it.first } && targetState.destination.route in items.map { it.first }) {
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300),
                        ) + fadeOut(animationSpec = tween(300))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route in items.map { it.first } && targetState.destination.route in items.map { it.first }) {
                        fadeIn(animationSpec = tween(300))
                    } else {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300),
                        ) + fadeIn(animationSpec = tween(300))
                    }
                },
                popExitTransition = {
                    if (initialState.destination.route in items.map { it.first } && targetState.destination.route in items.map { it.first }) {
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300),
                        ) + fadeOut(animationSpec = tween(300))
                    }
                },
            ) {
                composable("home") {
                    ChatScreen(
                        viewModel = viewModel,
                        onCameraClick = { onNavigateToCamera("home") },
                        onNavigateToTimeline = { query ->
                            navController.navigate("timeline?query=${android.net.Uri.encode(query)}")
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable("solver") {
                    val solverViewModel: com.example.solver.comprehensive.presentation.viewmodels.SolverViewModel = hiltViewModel()

                    // Observe the image URI returned from the outer NavHost (Camera/Preview flow)
                    val solverImageUri = outerSavedStateHandle?.getStateFlow<String?>("solver_image_uri", null)?.collectAsState()
                    LaunchedEffect(solverImageUri?.value) {
                        solverImageUri?.value?.let { uriString ->
                            solverViewModel.setImageUri(android.net.Uri.parse(uriString))
                            solverViewModel.solveProblem()
                            // Clear the saved state so it doesn't trigger again
                            outerSavedStateHandle?.remove<String>("solver_image_uri")
                        }
                    }

                    SolverScreen(
                        viewModel = solverViewModel,
                        onCameraClick = { onNavigateToCamera("solver") },
                    )
                }
                composable("summary") {
                    SummaryMenuScreen(
                        onNavigateToVideoSummary = { navController.navigate("video") },
                        onNavigateToTextSummary = { navController.navigate("text_summary") },
                        onNavigateToAudioSummary = { navController.navigate("audio_summary") },
                        onNavigateToChatSummary = { navController.navigate("dialogue_summary") },
                        onNavigateToKnowledgeCards = { navController.navigate("knowledge_cards") },
                    )
                }
                composable("review") {
                    val reviewViewModel: com.example.review.planner.presentation.viewmodels.ReviewViewModel = hiltViewModel()
                    ReviewScreen(reviewViewModel)
                }
                composable(
                    route = "timeline?query={query}",
                    arguments =
                        listOf(
                            androidx.navigation.navArgument("query") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                            },
                        ),
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query")
                    com.example.ai_tutor.timeline_map.presentation.screens.TimelineMapScreen(
                        initialQuery = query,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("video") { com.example.summarizer.videosummarizer.presentation.screens.VideoDownloadScreen(videoViewModel) }
                composable("text_summary") { TextSummaryScreenWrapper(textSummaryViewModel, navController) }
                composable("audio_summary") { AudioSummaryScreenWrapper(audioSummaryViewModel, navController) }
                composable("dialogue_summary") { DialogueSummaryScreenWrapper(navController) }
                composable("knowledge_cards") { KnowledgeCardScreenWrapper(navController) }
                composable("profile") { ProfileScreen() }
            }
        }
    }
}

@Composable
fun TextSummaryScreenWrapper(
    viewModel: TextSummaryViewModel,
    navController: androidx.navigation.NavHostController,
) {
    com.example.summarizer.text_summarizer.presentation.screens.TextSummaryScreen(
        viewModel = viewModel,
    )
}

@Composable
fun AudioSummaryScreenWrapper(
    viewModel: AudioSummaryViewModel,
    navController: androidx.navigation.NavHostController,
) {
    com.example.summarizer.audio_summarizer.presentation.screens.AudioSummaryScreen(
        viewModel = viewModel,
    )
}

@Composable
fun KnowledgeCardScreenWrapper(
    navController: androidx.navigation.NavHostController,
    viewModel: KnowledgeCardViewModel = hiltViewModel(),
) {
    KnowledgeCardScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
    )
}

@Composable
fun DialogueSummaryScreenWrapper(
    navController: androidx.navigation.NavHostController,
    viewModel: DialogueSummaryViewModel = hiltViewModel(),
) {
    DialogueSummaryScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
    )
}
