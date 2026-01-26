package com.example.ai_tutor.presentation.navigation

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
import com.example.ai_tutor.presentation.ChatScreen
import com.example.ai_tutor.presentation.auth.AuthViewModel
import com.example.ai_tutor.presentation.auth.LoginScreen
import com.example.ai_tutor.presentation.auth.RegisterScreen
import com.example.ai_tutor.presentation.screens.*
import androidx.lifecycle.viewmodel.compose.viewModel

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ai_tutor.presentation.AiTutorViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel(),
    aiTutorViewModel: AiTutorViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn = authViewModel.isLoggedIn.value

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main" else "login"
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
        composable("main") {
            MainScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onLogout = { 
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                },
                onNavigateToCamera = { navController.navigate("camera") },
                viewModel = aiTutorViewModel
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("camera") {
            CameraScreen(
                onImageCaptured = { uri ->
                    val encodedUri = java.net.URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate("preview/$encodedUri")
                },
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            "preview/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri") ?: ""
            ImagePreviewScreen(
                imageUri = imageUriString,
                onActionSelected = { prompt ->
                    aiTutorViewModel.sendImageWithPrompt(Uri.parse(imageUriString), prompt)
                    navController.popBackStack("main", inclusive = false)
                },
                onClose = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
