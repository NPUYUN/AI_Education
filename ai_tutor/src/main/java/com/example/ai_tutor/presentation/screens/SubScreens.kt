package com.example.ai_tutor.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.common.database.PreferencesManager
import kotlinx.coroutines.launch
import com.example.timeline_map.presentation.TimelineMapScreen
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
    TimelineMapScreen()
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoSummaryScreen(viewModel: VideoDownloadViewModel) {
    VideoDownloadScreen(viewModel = viewModel)
}

@Composable
fun ReviewScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Review (Coming Soon)")
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val themeMode by preferencesManager.getString("theme_mode", "auto").collectAsState(initial = "auto")

    SubScreenScaffold(title = "设置", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("主题设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            val themes = listOf(
                "auto" to "跟随系统",
                "light" to "浅色模式",
                "dark" to "深色模式"
            )
            
            themes.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { preferencesManager.saveString("theme_mode", mode) }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = {
                            scope.launch { preferencesManager.saveString("theme_mode", mode) }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }
            
            HorizontalDivider()
            
            Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("版本: 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
