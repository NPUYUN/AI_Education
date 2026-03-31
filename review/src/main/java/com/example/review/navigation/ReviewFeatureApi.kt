package com.example.review.navigation

import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.common.navigation.FeatureApi
import com.example.review.planner.presentation.screens.ReviewScreen
import com.example.review.planner.presentation.viewmodels.ReviewViewModel
import javax.inject.Inject

class ReviewFeatureApi
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
            navGraphBuilder.composable("review") {
                val reviewViewModel: ReviewViewModel = hiltViewModel()
                ReviewScreen(reviewViewModel)
            }
        }
    }
