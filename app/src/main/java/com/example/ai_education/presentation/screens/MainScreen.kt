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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ai_tutor.multimodal_chat.presentation.viewmodels.AiTutorViewModel
import com.example.common.R
import kotlinx.coroutines.launch
import java.io.File

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToCamera: (String) -> Unit,
    onNavigateToImagePreview: (String, String) -> Unit,
    viewModel: AiTutorViewModel,
    outerSavedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
    aiTutorFeatureApi: com.example.ai_tutor.navigation.AiTutorFeatureApi,
    solverFeatureApi: com.example.solver.navigation.SolverFeatureApi,
    summarizerFeatureApi: com.example.summarizer.navigation.SummarizerFeatureApi,
    reviewFeatureApi: com.example.review.navigation.ReviewFeatureApi,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember { com.example.common.database.PreferencesManager(context) }
    val savedNickname by preferencesManager.getString(
        "user_nickname",
        stringResource(R.string.user_nickname),
    ).collectAsStateWithLifecycle(initialValue = stringResource(R.string.user_nickname))
    val savedAvatar by preferencesManager.getString("user_avatar", "").collectAsStateWithLifecycle(initialValue = "")

    val sessions by viewModel.sessions.collectAsStateWithLifecycle(
        initialValue = emptyList(),
    )

    // Bottom Navigation Items
    // Order: AI Tutor, Solver, Summary, Review
    val items =
        listOf(
            Triple("home", Icons.Default.Face, stringResource(R.string.ai_tutor_alt)),
            Triple("solver", Icons.Default.Edit, stringResource(R.string.problem_solving)),
            Triple("summary", Icons.Default.Summarize, stringResource(R.string.summarize)),
            Triple("review", Icons.Default.RateReview, stringResource(R.string.review)),
        )

    val searchFeatureComingSoonMsg = stringResource(R.string.search_feature_coming_soon)
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
                                    android.widget.Toast.makeText(
                                        context,
                                        searchFeatureComingSoonMsg,
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.search), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_conversation),
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
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = savedNickname.ifBlank { stringResource(R.string.user_nickname) },
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
                                text = if (session.title.isEmpty()) stringResource(R.string.new_conversation) else session.title,
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
                                text = stringResource(R.string.settings),
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
                            val noNewNotificationsMsg = stringResource(R.string.no_new_notifications)
                            Text(
                                text = stringResource(R.string.notifications),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            android.widget.Toast.makeText(
                                                context,
                                                noNewNotificationsMsg,
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                            )
                        }

                        item {
                            Text(
                                text = stringResource(R.string.logout),
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
                    val noNewNotificationsMsg = stringResource(R.string.no_new_notifications)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications),
                                modifier =
                                    Modifier.clickable {
                                        android.widget.Toast.makeText(
                                            context,
                                            noNewNotificationsMsg,
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                            )
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
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
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                // Show top bar on main tabs and profile page
                val isMainTab = items.any { it.first == currentRoute } || currentRoute == "review_menu"
                if ((isMainTab && currentRoute != "timeline" && currentRoute != "video") || currentRoute == "profile") {
                    CenterAlignedTopAppBar(
                        title = {
                            val label =
                                if (currentRoute == "profile") {
                                    stringResource(R.string.personal_homepage)
                                } else if (currentRoute == "review_menu") {
                                    stringResource(R.string.review)
                                } else {
                                    items.find {
                                        it.first == currentRoute
                                    }?.third ?: stringResource(R.string.ai_tutor)
                                }
                            Text(label)
                        },
                        navigationIcon = {
                            if (isMainTab && currentRoute == "home") {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.open_menu))
                                }
                            } else if (!isMainTab) {
                                IconButton(onClick = {
                                    navController.popBackStack()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            }
                        },
                        actions = {
                            if (currentRoute == "home") {
                                val shareConversationMsg = stringResource(R.string.share_conversation)
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
                                        val chooser = android.content.Intent.createChooser(sendIntent, shareConversationMsg)
                                        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        try {
                                            context.startActivity(chooser)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                                }
                            } else if (currentRoute == "profile") {
                                IconButton(onClick = {
                                    onNavigateToSettings()
                                }) {
                                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                                }
                            }
                        },
                    )
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                if (items.any { it.first == currentRoute } || currentRoute == "review_menu") {
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
                    val isInitialMain = initialState.destination.route in items.map { it.first } || initialState.destination.route == "review_menu"
                    val isTargetMain = targetState.destination.route in items.map { it.first } || targetState.destination.route == "review_menu"
                    if (isInitialMain && isTargetMain) {
                        fadeIn(animationSpec = tween(300))
                    } else {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300),
                        ) + fadeIn(animationSpec = tween(300))
                    }
                },
                exitTransition = {
                    val isInitialMain = initialState.destination.route in items.map { it.first } || initialState.destination.route == "review_menu"
                    val isTargetMain = targetState.destination.route in items.map { it.first } || targetState.destination.route == "review_menu"
                    if (isInitialMain && isTargetMain) {
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300),
                        ) + fadeOut(animationSpec = tween(300))
                    }
                },
                popEnterTransition = {
                    val isInitialMain = initialState.destination.route in items.map { it.first } || initialState.destination.route == "review_menu"
                    val isTargetMain = targetState.destination.route in items.map { it.first } || targetState.destination.route == "review_menu"
                    if (isInitialMain && isTargetMain) {
                        fadeIn(animationSpec = tween(300))
                    } else {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300),
                        ) + fadeIn(animationSpec = tween(300))
                    }
                },
                popExitTransition = {
                    val isInitialMain = initialState.destination.route in items.map { it.first } || initialState.destination.route == "review_menu"
                    val isTargetMain = targetState.destination.route in items.map { it.first } || targetState.destination.route == "review_menu"
                    if (isInitialMain && isTargetMain) {
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300),
                        ) + fadeOut(animationSpec = tween(300))
                    }
                },
            ) {
                aiTutorFeatureApi.registerGraph(
                    navGraphBuilder = this,
                    navController = navController,
                    onNavigateToCamera = onNavigateToCamera,
                    onNavigateToImagePreview = onNavigateToImagePreview,
                    outerSavedStateHandle = outerSavedStateHandle,
                    sharedViewModel = viewModel,
                )

                solverFeatureApi.registerGraph(
                    navGraphBuilder = this,
                    navController = navController,
                    onNavigateToCamera = onNavigateToCamera,
                    onNavigateToImagePreview = onNavigateToImagePreview,
                    outerSavedStateHandle = outerSavedStateHandle,
                )

                summarizerFeatureApi.registerGraph(
                    navGraphBuilder = this,
                    navController = navController,
                    onNavigateToCamera = onNavigateToCamera,
                    onNavigateToImagePreview = onNavigateToImagePreview,
                    outerSavedStateHandle = outerSavedStateHandle,
                )

                reviewFeatureApi.registerGraph(
                    navGraphBuilder = this,
                    navController = navController,
                    onNavigateToCamera = onNavigateToCamera,
                    onNavigateToImagePreview = onNavigateToImagePreview,
                    outerSavedStateHandle = outerSavedStateHandle,
                )

                composable("profile") {
                    ProfileScreen()
                }
            }
        }
    }
}
