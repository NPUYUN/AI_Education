package com.example.common.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

interface FeatureApi {
    fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier = Modifier,
        // Optional dependencies passed from app module
        onNavigateToCamera: (String) -> Unit = {},
        onNavigateToImagePreview: (String, String) -> Unit = { _, _ -> },
        outerSavedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
        // Since AiTutorViewModel is scoped to MainScreen currently, we pass it down.
        // In a fully decoupled app, it should be hiltViewModel() inside the graph.
        sharedViewModel: Any? = null,
    )
}
