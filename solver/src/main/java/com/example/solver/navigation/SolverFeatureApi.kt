package com.example.solver.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.common.navigation.FeatureApi
import com.example.solver.comprehensive.presentation.screens.SolverScreen
import com.example.solver.comprehensive.presentation.viewmodels.SolverViewModel
import javax.inject.Inject

class SolverFeatureApi
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
            navGraphBuilder.composable("solver") {
                val solverViewModel: SolverViewModel = hiltViewModel()

                // Observe the image URI returned from the outer NavHost (Camera/Preview flow)
                val solverImageUri = outerSavedStateHandle?.getStateFlow<String?>("solver_image_uri", null)?.collectAsStateWithLifecycle()
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
        }
    }
