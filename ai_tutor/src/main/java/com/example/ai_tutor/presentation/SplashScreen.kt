package com.example.ai_tutor.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ai_tutor.presentation.manager.VoskModelManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onLoadComplete: () -> Unit) {
    val context = LocalContext.current
    // Collect the initialization state from the manager
    val initState by VoskModelManager.initState.collectAsState()

    // Start initialization when the screen launches
    LaunchedEffect(Unit) {
        VoskModelManager.initModel(context)
    }

    // Auto-navigate when ready
    LaunchedEffect(initState) {
        if (initState is VoskModelManager.InitState.Ready) {
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

            when (val state = initState) {
                is VoskModelManager.InitState.Idle -> {
                    Text("准备中...")
                }
                is VoskModelManager.InitState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在下载语音模型... ${(state.progress * 100).toInt()}%")
                }
                is VoskModelManager.InitState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在加载模型...")
                }
                is VoskModelManager.InitState.Ready -> {
                    Text("加载完成")
                }
                is VoskModelManager.InitState.Error -> {
                    Text(
                        text = "加载失败: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { VoskModelManager.initModel(context) }) {
                        Text("重试")
                    }
                }
            }
            
            // Background Download Button (Visible when not ready)
            if (initState !is VoskModelManager.InitState.Ready) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { onLoadComplete() }
                ) {
                    Text("后台下载 (进入主页)")
                }
            }
        }
    }
}
