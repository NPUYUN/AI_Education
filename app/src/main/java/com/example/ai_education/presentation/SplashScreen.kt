package com.example.ai_education.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.common.dispatchers.DefaultDispatcherProvider
import com.example.common.manager.VoskModelManager
import com.example.video_summarizer.data.downloader.ModelDownloader
import com.example.video_summarizer.data.downloader.DownloadProgress
import com.example.video_summarizer.data.downloader.DownloadStatus
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onLoadComplete: () -> Unit) {
    val context = LocalContext.current
    // Collect the initialization state from the manager
    val voskState by VoskModelManager.initState.collectAsState()
    
    var sherpaProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    var sherpaReady by remember { mutableStateOf(false) }
    var sherpaError by remember { mutableStateOf<String?>(null) }
    var sherpaRetryTrigger by remember { mutableIntStateOf(0) }

    // Start initialization when the screen launches
    LaunchedEffect(Unit) {
        VoskModelManager.initModel(context)
    }

    LaunchedEffect(sherpaRetryTrigger) {
        sherpaError = null
        val sherpaDownloader = ModelDownloader(context, DefaultDispatcherProvider())
        if (!sherpaDownloader.isModelReady()) {
            sherpaDownloader.downloadAndExtractModel { progress ->
                sherpaProgress = progress
            }.fold(
                onSuccess = {
                    sherpaReady = true
                },
                onFailure = { error ->
                    sherpaError = error.message
                }
            )
        } else {
            sherpaReady = true
        }
    }

    // Auto-navigate when ready
    LaunchedEffect(voskState, sherpaReady) {
        if (voskState is VoskModelManager.InitState.Ready && sherpaReady) {
            delay(500)
            onLoadComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "AI 辅导助手",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在准备运行环境...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Vosk Model Card
            ModelStatusCard(
                title = "语音唤醒模型",
                status = when (val state = voskState) {
                    is VoskModelManager.InitState.Idle -> ModelStatus.Preparing
                    is VoskModelManager.InitState.Downloading -> ModelStatus.Downloading(state.progress)
                    is VoskModelManager.InitState.Loading -> ModelStatus.Processing
                    is VoskModelManager.InitState.Ready -> ModelStatus.Ready
                    is VoskModelManager.InitState.Error -> ModelStatus.Error(state.message)
                },
                onRetry = { VoskModelManager.initModel(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sherpa Model Card
            ModelStatusCard(
                title = "语音识别模型",
                status = if (sherpaReady) ModelStatus.Ready
                else if (sherpaError != null) ModelStatus.Error(sherpaError!!)
                else {
                    val progress = sherpaProgress
                    if (progress == null || progress.status == DownloadStatus.PREPARING) ModelStatus.Preparing
                    else if (progress.status == DownloadStatus.DOWNLOADING) ModelStatus.Downloading(progress.progress / 100f)
                    else ModelStatus.Processing
                },
                onRetry = { sherpaRetryTrigger++ }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Background Download Button
            if (voskState !is VoskModelManager.InitState.Ready || !sherpaReady) {
                TextButton(
                    onClick = { onLoadComplete() }
                ) {
                    Text("后台下载并进入主页")
                }
            }
        }
    }
}

sealed class ModelStatus {
    object Preparing : ModelStatus()
    data class Downloading(val progress: Float) : ModelStatus()
    object Processing : ModelStatus()
    object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

@Composable
fun ModelStatusCard(
    title: String,
    status: ModelStatus,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                
                when (status) {
                    is ModelStatus.Ready -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Ready",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    is ModelStatus.Error -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            if (status !is ModelStatus.Ready) {
                Spacer(modifier = Modifier.height(12.dp))
                when (status) {
                    is ModelStatus.Preparing -> Text("准备中...", style = MaterialTheme.typography.bodyMedium)
                    is ModelStatus.Processing -> Text("正在处理...", style = MaterialTheme.typography.bodyMedium)
                    is ModelStatus.Downloading -> {
                        val animatedProgress by animateFloatAsState(targetValue = status.progress)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("下载中...", style = MaterialTheme.typography.bodySmall)
                                Text("${(status.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                    is ModelStatus.Error -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "失败: ${status.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                                Text("重试")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

