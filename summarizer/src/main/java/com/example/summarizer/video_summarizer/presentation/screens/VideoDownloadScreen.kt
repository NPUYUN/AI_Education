package com.example.summarizer.video_summarizer.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.Manifest
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.ui.PlayerView
import java.io.File
import com.example.common.presentation.components.GlobalApiSettingsDialog
import com.example.summarizer.video_summarizer.services.DownloadProgress
import com.example.summarizer.video_summarizer.services.DownloadStatus
import com.example.summarizer.video_summarizer.presentation.viewmodels.DownloadTask
import com.example.summarizer.video_summarizer.presentation.viewmodels.SummaryStatus
import com.example.summarizer.video_summarizer.presentation.viewmodels.VideoDownloadViewModel

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDownloadScreen(
    viewModel: VideoDownloadViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadTasks = uiState.downloadTasks
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val localVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleLocalVideo(it) }
    }

    var playingFile by remember { mutableStateOf<File?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings || uiState.showApiSettings) {
        GlobalApiSettingsDialog(
            onDismiss = {
                showSettings = false
                viewModel.setApiSettingsVisible(false)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频下载") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            UrlInputCard(
                url = uiState.inputUrl,
                onUrlChange = viewModel::updateInputUrl,
                onDownloadClick = {
                    if (uiState.inputUrl.isNotBlank()) {
                        viewModel.addDownloadTask(uiState.inputUrl)
                    }
                },
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            LocalVideoInputCard(
                onPickVideo = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions)
                    localVideoLauncher.launch("video/*")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            uiState.error?.let { error ->
                ErrorCard(
                    message = error,
                    onDismiss = viewModel::clearError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            uiState.successMessage?.let { message ->
                SuccessCard(
                    message = message,
                    onDismiss = viewModel::clearSuccess
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (downloadTasks.isNotEmpty()) {
                Text(
                    text = "下载任务 (${downloadTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(downloadTasks) { task ->
                        DownloadTaskCard(
                            task = task,
                            onCancel = { viewModel.cancelDownload(task.id) },
                            onRemove = { viewModel.removeTask(task.id) },
                            onPlay = {
                                task.localPath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) {
                                        playingFile = file
                                    }
                                }
                            },
                            onOpenFolder = {
                                task.localPath?.let { path ->
                                    val file = File(path)
                                    if (!file.exists()) return@let
                                    val parent = file.parentFile ?: return@let
                                    try {
                                        val folderUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            parent
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(folderUri, "resource/folder")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "video/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                "无法打开文件夹：${parent.absolutePath}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onSummarize = {
                                viewModel.startVideoSummary(task.id, task.localPath)
                            }
                        )
                    }
                }
            } else {
                EmptyStateCard()
            }
        }
    }

    playingFile?.let { file ->
        VideoPlayerDialog(
            file = file,
            onDismiss = { playingFile = null }
        )
    }

}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoPlayerDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val videoUri = remember(file) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val openExternalPlayer = remember(videoUri) {
        {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(videoUri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "无法打开系统播放器",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val player = remember(videoUri) {
        val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "无法在应用内播放该视频"
                openExternalPlayer()
                onDismiss()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 520.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        setUseController(true)
                        keepScreenOn = true
                        this.player = player
                    }
                },
                update = { view ->
                    if (view.player !== player) {
                        view.player = player
                    }
                }
            )
        }
    }
}

@Composable
fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onDownloadClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "添加下载任务",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请粘贴B站、YouTube、抖音等视频链接") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                trailingIcon = {
                    if (url.isNotBlank()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始下载")
            }
        }
    }
}

@Composable
fun LocalVideoInputCard(
    onPickVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "本地视频上传",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onPickVideo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择本地视频并生成摘要")
            }
        }
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit,
    onOpenFolder: () -> Unit,
    onSummarize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TaskStatusBadge(status = task.progress.status)
            }

            if (task.progress.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    LinearProgressIndicator(
                        progress = { task.progress.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${task.progress.progress.toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${task.progress.currentSpeed} · ${task.progress.eta}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (task.summary.status != SummaryStatus.IDLE) {
                Spacer(modifier = Modifier.height(12.dp))
                SummaryStatusBadge(status = task.summary.status)
                task.summary.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (task.summary.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "摘要",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.summary.summary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (task.progress.status == DownloadStatus.DOWNLOADING && task.id != "model_download_task") {
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("取消")
                    }
                }

                if (task.progress.status == DownloadStatus.COMPLETED) {
                    if (task.id != "model_download_task") {
                        TextButton(onClick = onPlay) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("在线播放")
                        }
                        TextButton(onClick = onOpenFolder) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("打开文件夹")
                        }
                        TextButton(onClick = onSummarize) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (task.summary.status == SummaryStatus.COMPLETED) "重新摘要" else "生成摘要")
                        }
                    }
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("移除")
                    }
                }

                if (task.progress.status == DownloadStatus.FAILED ||
                    task.progress.status == DownloadStatus.CANCELLED) {
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("移除")
                    }
                }
            }
        }
    }
}

@Composable
fun TaskStatusBadge(status: DownloadStatus) {
    val (color, text) = when (status) {
        DownloadStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to "等待中"
        DownloadStatus.PREPARING -> MaterialTheme.colorScheme.primary to "准备中"
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary to "下载中"
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary to "已完成"
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error to "失败"
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.secondary to "已取消"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SummaryStatusBadge(status: SummaryStatus) {
    val (color, text) = when (status) {
        SummaryStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to "未开始"
        SummaryStatus.PREPARING -> MaterialTheme.colorScheme.primary to "准备中"
        SummaryStatus.TRANSCRIBING -> MaterialTheme.colorScheme.primary to "转写中"
        SummaryStatus.SUMMARIZING -> MaterialTheme.colorScheme.primary to "摘要中"
        SummaryStatus.COMPLETED -> MaterialTheme.colorScheme.primary to "摘要完成"
        SummaryStatus.FAILED -> MaterialTheme.colorScheme.error to "摘要失败"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun SuccessCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无下载任务",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "粘贴视频链接即可开始下载",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


