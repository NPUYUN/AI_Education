package com.example.review.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.common.navigation.FeatureApi
import com.example.review.planner.presentation.screens.ErrorBookScreen
import com.example.review.planner.presentation.screens.KnowledgeReinforcementScreen
import com.example.review.planner.presentation.screens.ReviewMenuScreen
import com.example.review.planner.presentation.screens.SmartReviewPlannerScreen
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
            onNavigateToImagePreview: (String, String) -> Unit,
            outerSavedStateHandle: androidx.lifecycle.SavedStateHandle?,
            sharedViewModel: Any?,
        ) {
            navGraphBuilder.navigation(route = "review", startDestination = "review_menu") {
                composable("review_menu") { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry("review")
                        }
                    val reviewViewModel: ReviewViewModel = hiltViewModel(parentEntry)
                    ReviewMenuScreen(
                        onNavigateToPlanner = { navController.navigate("review_planner") },
                        onNavigateToReinforcement = { navController.navigate("review_reinforcement") },
                        onNavigateToErrorBook = { navController.navigate("review_error_book") },
                    )
                }

                composable("review_planner") { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry("review")
                        }
                    val reviewViewModel: ReviewViewModel = hiltViewModel(parentEntry)
                    SmartReviewPlannerScreen(
                        viewModel = reviewViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable("review_reinforcement") { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry("review")
                        }
                    val reviewViewModel: ReviewViewModel = hiltViewModel(parentEntry)
                    KnowledgeReinforcementScreen(
                        viewModel = reviewViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable("review_error_book") { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry("review")
                        }
                    val reviewViewModel: ReviewViewModel = hiltViewModel(parentEntry)
                    ErrorBookScreen(
                        viewModel = reviewViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
