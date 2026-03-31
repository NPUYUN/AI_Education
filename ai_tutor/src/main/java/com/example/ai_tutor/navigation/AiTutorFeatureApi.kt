package com.example.ai_tutor.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ai_tutor.multimodal_chat.presentation.screens.ChatScreen
import com.example.ai_tutor.multimodal_chat.presentation.viewmodels.AiTutorViewModel
import com.example.ai_tutor.timeline_map.presentation.screens.TimelineMapScreen
import com.example.common.navigation.FeatureApi
import javax.inject.Inject

class AiTutorFeatureApi
    @Inject
    constructor() : FeatureApi {
        override fun registerGraph(
            navGraphBuilder: NavGraphBuilder,
            navController: NavHostController,
            modifier: Modifier,
            onNavigateToCamera: (String) -> Unit,
            outerSavedStateHandle: androidx.lifecycle.SavedStateHandle?,
            sharedViewModel: Any?,
        ) {
            navGraphBuilder.composable("home") {
                // Safe cast since we know it's passed from app module
                val viewModel = sharedViewModel as? AiTutorViewModel
                if (viewModel != null) {
                    ChatScreen(
                        viewModel = viewModel,
                        onCameraClick = { onNavigateToCamera("home") },
                        onNavigateToTimeline = { query ->
                            navController.navigate("timeline?query=${android.net.Uri.encode(query)}")
                        },
                        modifier = modifier.fillMaxSize(),
                    )
                }
            }

            navGraphBuilder.composable(
                route = "timeline?query={query}",
                arguments =
                    listOf(
                        navArgument("query") {
                            type = NavType.StringType
                            nullable = true
                        },
                    ),
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query")
                TimelineMapScreen(
                    initialQuery = query,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
