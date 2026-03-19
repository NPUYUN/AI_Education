package com.example.ai_tutor.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        val sherpaDownloader = ModelDownloader(context)
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "AI 辅导助手",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Vosk Model Status
            when (val state = voskState) {
                is VoskModelManager.InitState.Idle -> {
                    Text("语音唤醒模型准备中...")
                }
                is VoskModelManager.InitState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在下载语音唤醒模型... ${(state.progress * 100).toInt()}%")
                }
                is VoskModelManager.InitState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在加载语音唤醒模型...")
                }
                is VoskModelManager.InitState.Ready -> {
                    Text("语音唤醒模型就绪")
                }
                is VoskModelManager.InitState.Error -> {
                    Text(
                        text = "语音唤醒模型失败: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { VoskModelManager.initModel(context) }) {
                        Text("重试唤醒模型")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sherpa Model Status
            if (sherpaReady) {
                Text("语音识别模型就绪")
            } else if (sherpaError != null) {
                Text(
                    text = "语音识别模型失败: $sherpaError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { 
                    sherpaRetryTrigger++
                }) {
                    Text("重试识别模型")
                }
            } else {
                val progress = sherpaProgress
                if (progress == null || progress.status == DownloadStatus.PREPARING) {
                    Text("语音识别模型准备中...")
                } else if (progress.status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { progress.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在下载语音识别模型... ${progress.progress.toInt()}%")
                } else {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在处理语音识别模型...")
                }
            }
            
            // Background Download Button (Visible when not ready)
            if (voskState !is VoskModelManager.InitState.Ready || !sherpaReady) {
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { onLoadComplete() }
                ) {
                    Text("后台下载 (进入主页)")
                }
            }
        }
    }
}
