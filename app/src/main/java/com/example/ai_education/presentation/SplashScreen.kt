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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.common.dispatchers.DefaultDispatcherProvider
import com.example.common.manager.VoskModelManager
import com.example.summarizer.data.downloader.ModelDownloader
import com.example.summarizer.data.downloader.DownloadProgress
import com.example.summarizer.data.downloader.DownloadStatus
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onLoadComplete: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Collect the initialization state from the manager
    val voskState by viewModel.voskModelManager.initState.collectAsState()
    
    val sherpaProgress by viewModel.sherpaProgress.collectAsState()
    val sherpaReady by viewModel.sherpaReady.collectAsState()
    val sherpaError by viewModel.sherpaError.collectAsState()

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

            val currentTaskTitle = if (voskState !is VoskModelManager.InitState.Ready) {
                "正在加载: 语音唤醒模型"
            } else if (!sherpaReady) {
                "正在加载: 语音识别模型"
            } else {
                "加载完成"
            }

            val currentTaskStatus = if (voskState !is VoskModelManager.InitState.Ready) {
                when (val state = voskState) {
                    is VoskModelManager.InitState.Idle -> ModelStatus.Preparing
                    is VoskModelManager.InitState.Downloading -> ModelStatus.Downloading(state.progress)
                    is VoskModelManager.InitState.Loading -> ModelStatus.Processing
                    is VoskModelManager.InitState.Error -> ModelStatus.Error(state.message)
                    else -> ModelStatus.Preparing
                }
            } else {
                if (sherpaReady) ModelStatus.Ready
                else if (sherpaError != null) ModelStatus.Error(sherpaError!!)
                else {
                    val progress = sherpaProgress
                    if (progress == null || progress.status == DownloadStatus.PREPARING) ModelStatus.Preparing
                    else if (progress.status == DownloadStatus.DOWNLOADING) ModelStatus.Downloading(progress.progress / 100f)
                    else ModelStatus.Processing
                }
            }

            val onRetry = if (voskState !is VoskModelManager.InitState.Ready) {
                { viewModel.retryVosk() }
            } else {
                { viewModel.initSherpaModel() }
            }

            // Single unified progress card
            ModelStatusProgress(
                title = currentTaskTitle,
                status = currentTaskStatus,
                onRetry = onRetry
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
fun ModelStatusProgress(
    title: String,
    status: ModelStatus,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
                is ModelStatus.Downloading -> {
                    Text("${(status.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    // Show indeterminate state text
                    Text(
                        text = if (status is ModelStatus.Preparing) "准备中..." else "处理中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (status is ModelStatus.Error) {
            Text(
                text = "失败: ${status.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("重试")
            }
        } else {
            val animatedProgress by animateFloatAsState(
                targetValue = if (status is ModelStatus.Downloading) status.progress else if (status is ModelStatus.Ready) 1f else 0f,
                label = "progress_animation"
            )
            
            if (status is ModelStatus.Downloading || status is ModelStatus.Ready) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

