package com.example.ai_education.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai_tutor.multimodal_chat.presentation.screens.ChatScreen
import com.example.ai_education.presentation.auth.AuthViewModel
import com.example.ai_education.presentation.auth.LoginScreen
import com.example.ai_education.presentation.auth.RegisterScreen
import com.example.ai_education.presentation.screens.*
import com.example.common.presentation.camera.CameraScreen
import com.example.common.presentation.camera.ImagePreviewScreen
import androidx.lifecycle.viewmodel.compose.viewModel

import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ai_tutor.multimodal_chat.presentation.viewmodels.AiTutorViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    aiTutorViewModel: AiTutorViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn = authViewModel.isLoggedIn.value

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main" else "login",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("main") { popUpTo("register") { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("main") { backStackEntry ->
            MainScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onLogout = { 
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                },
                onNavigateToCamera = { source -> navController.navigate("camera?source=$source") },
                viewModel = aiTutorViewModel,
                outerSavedStateHandle = backStackEntry.savedStateHandle
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("camera?source={source}", arguments = listOf(navArgument("source") { defaultValue = "home" })) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "home"
            CameraScreen(
                source = source,
                onImageCaptured = { uri ->
                    val encodedUri = java.net.URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate("preview/$encodedUri?source=$source")
                },
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            "preview/{imageUri}?source={source}",
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("source") { defaultValue = "home" }
            )
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri") ?: ""
            val source = backStackEntry.arguments?.getString("source") ?: "home"
            ImagePreviewScreen(
                imageUri = imageUriString,
                source = source,
                onActionSelected = { prompt, uri ->
                    if (source == "solver") {
                        navController.getBackStackEntry("main").savedStateHandle.set("solver_image_uri", uri.toString())
                        navController.popBackStack("main", inclusive = false)
                    } else {
                        aiTutorViewModel.sendImageWithPrompt(uri, prompt)
                        navController.popBackStack("main", inclusive = false)
                    }
                },
                onClose = { navController.popBackStack() }
            )
        }
    }
}

