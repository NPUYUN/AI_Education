package com.example.ai_education.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.automirrored.filled.Article
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
import com.example.ai_tutor.presentation.TimelineMapScreen
import com.example.summarizer.presentation.VideoDownloadViewModel
import com.example.summarizer.presentation.screens.VideoDownloadScreen

import com.example.common.presentation.components.GlobalApiSettingsDialog

@Composable
fun GeometryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Geometry Solver (Coming Soon)")
    }
}

@Composable
fun TimelineScreen(navController: NavController, query: String? = null) {
    TimelineMapScreen(
        initialQuery = query,
        onNavigateBack = { navController.popBackStack() }
    )
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoSummaryScreen(viewModel: VideoDownloadViewModel, navController: NavController) {
    SubScreenScaffold(title = "视频总结", onBack = { navController.popBackStack() }) {
        VideoDownloadScreen(viewModel = viewModel)
    }
}

@Composable
fun SummaryMenuScreen(navController: NavController) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable { navController.navigate("video") },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "视频总结", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(24.dp))
                Text("视频总结", style = MaterialTheme.typography.titleLarge)
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable { android.widget.Toast.makeText(context, "文本总结等其他形式即将上线", android.widget.Toast.LENGTH_SHORT).show() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "文本总结", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(24.dp))
                Text("文本总结", style = MaterialTheme.typography.titleLarge)
            }
        }
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
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApiSettings = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("全局大模型设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("配置各模块的 API Key、模型和 Base URL", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

