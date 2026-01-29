package com.example.ai_tutor.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai_tutor.presentation.navigation.SubScreenScaffold
import com.example.video_summarizer.presentation.VideoDownloadViewModel
import com.example.video_summarizer.presentation.screens.VideoDownloadScreen

@Composable
fun GeometryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Geometry Solver (Coming Soon)")
    }
}

@Composable
fun TimelineScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Timeline Map (Coming Soon)")
    }
}

@Composable
fun VideoSummaryScreen() {
    val viewModel: VideoDownloadViewModel = viewModel()
    VideoDownloadScreen(viewModel = viewModel)
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    SubScreenScaffold(title = "Settings", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Settings Page")
        }
    }
}
