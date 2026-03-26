package com.example.ai_education.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ai_tutor.timeline_map.presentation.screens.TimelineMapScreen
import com.example.common.database.PreferencesManager
import com.example.common.presentation.components.GlobalApiSettingsDialog
import com.example.summarizer.videosummarizer.presentation.screens.VideoDownloadScreen
import com.example.summarizer.videosummarizer.presentation.viewmodels.VideoDownloadViewModel
import kotlinx.coroutines.launch

@Composable
fun TimelineScreen(
    navController: NavController,
    query: String? = null,
) {
    TimelineMapScreen(
        initialQuery = query,
        onNavigateBack = { navController.popBackStack() },
    )
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoSummaryScreen(
    viewModel: VideoDownloadViewModel,
    navController: NavController,
) {
    SubScreenScaffold(title = "视频总结", onBack = { navController.popBackStack() }) {
        VideoDownloadScreen(viewModel = viewModel)
    }
}

@Composable
fun TextSummaryScreenWrapper(
    viewModel: com.example.summarizer.text_summarizer.presentation.viewmodels.TextSummaryViewModel,
    navController: NavController,
) {
    SubScreenScaffold(title = "文本总结", onBack = { navController.popBackStack() }) {
        com.example.summarizer.text_summarizer.presentation.screens.TextSummaryScreen(viewModel = viewModel)
    }
}

@Composable
fun AudioSummaryScreenWrapper(
    viewModel: com.example.summarizer.audio_summarizer.presentation.viewmodels.AudioSummaryViewModel,
    navController: NavController,
) {
    SubScreenScaffold(title = "音频总结", onBack = { navController.popBackStack() }) {
        com.example.summarizer.audio_summarizer.presentation.screens.AudioSummaryScreen(viewModel = viewModel)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val themeMode by preferencesManager.getString("theme_mode", "auto").collectAsState(initial = "auto")
    var showApiSettings by remember { mutableStateOf(false) }

    if (showApiSettings) {
        GlobalApiSettingsDialog(onDismiss = { showApiSettings = false })
    }

    SubScreenScaffold(title = "设置", onBack = onBack) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("主题设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val themes =
                listOf(
                    "auto" to "跟随系统",
                    "light" to "浅色模式",
                    "dark" to "深色模式",
                )

            themes.forEach { (mode, label) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { preferencesManager.saveString("theme_mode", mode) }
                            }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = {
                            scope.launch { preferencesManager.saveString("theme_mode", mode) }
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showApiSettings = true }
                        .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("全局大模型设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "配置各模块的 API Key、模型和 Base URL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
