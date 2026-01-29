package com.example.ai_tutor.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ai_tutor.presentation.navigation.SubScreenScaffold
import com.example.timeline_map.presentation.TimelineMapScreen

@Composable
fun GeometryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Geometry Solver (Coming Soon)")
    }
}

@Composable
fun TimelineScreen() {
    TimelineMapScreen()
}

@Composable
fun VideoSummaryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Video Summary (Coming Soon)")
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    SubScreenScaffold(title = "Settings", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Settings Page")
        }
    }
}
