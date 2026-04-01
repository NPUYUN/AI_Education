package com.example.summarizer.videosummarizer.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.common.R
import com.example.common.presentation.components.GlobalApiSettingsDialog
import com.example.summarizer.videosummarizer.presentation.viewmodels.DownloadTask
import com.example.summarizer.videosummarizer.presentation.viewmodels.SummaryStatus
import com.example.summarizer.videosummarizer.presentation.viewmodels.VideoDownloadViewModel
import com.example.summarizer.videosummarizer.services.DownloadStatus
import kotlinx.coroutines.launch
import java.io.File

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoDownloadScreen(
    viewModel: VideoDownloadViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadTasks = uiState.downloadTasks
    val context = LocalContext.current

    val localVideoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { viewModel.handleLocalVideo(it) }
        }

    var playingFile by remember { mutableStateOf<File?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)
    val successEvent by viewModel.successEvents.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(errorEvent) {
        errorEvent?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Long,
            )
        }
    }

    LaunchedEffect(successEvent) {
        successEvent?.let { successMsg ->
            snackbarHostState.showSnackbar(
                message = successMsg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    if (showSettings || uiState.showApiSettings) {
        GlobalApiSettingsDialog(
            onDismiss = {
                showSettings = false
                viewModel.setApiSettingsVisible(false)
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.video_summary)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            UrlInputCard(
                url = uiState.inputUrl,
                onUrlChange = viewModel::updateInputUrl,
                onDownloadClick = {
                    if (uiState.inputUrl.isNotBlank()) {
                        viewModel.addDownloadTask(uiState.inputUrl)
                    }
                },
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LocalVideoInputCard(
                onPickVideo = {
                    localVideoLauncher.launch("video/*")
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error and Success handling moved to Snackbar

            if (downloadTasks.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.download_tasks_count, downloadTasks.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(
                        items = downloadTasks,
                        key = { it.id },
                    ) { task ->
                        DownloadTaskCard(
                            task = task,
                            modifier = Modifier.animateItemPlacement(),
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
                                        val folderUri =
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                parent,
                                            )
                                        val intent =
                                            Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(folderUri, "resource/folder")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val uri =
                                                FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    file,
                                                )
                                            val intent =
                                                Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.cannot_open_folder, parent.absolutePath),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onSummarize = {
                                viewModel.startVideoSummary(task.id, task.localPath)
                            },
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
            onDismiss = { playingFile = null },
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("历史记录") },
            text = {
                if (uiState.historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("暂无历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(items = uiState.historyList) { history ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                        Text(
                                            text = sdf.format(java.util.Date(history.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteHistory(history) },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = history.sourceTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = history.summaryResult,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoPlayerDialog(
    file: File,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val videoUri =
        remember(file) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file,
            )
        }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val cannotOpenPlayerMsg = stringResource(R.string.cannot_open_system_player)
    val cannotPlayVideoMsg = stringResource(R.string.cannot_play_video_in_app)
    val openExternalPlayer =
        remember(videoUri) {
            {
                try {
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(videoUri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        cannotOpenPlayerMsg,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

    val player =
        remember(videoUri) {
            val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            ExoPlayer.Builder(context, renderersFactory).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = true
            }
        }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = cannotPlayVideoMsg
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 520.dp),
            shape = RoundedCornerShape(12.dp),
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
                },
            )
        }
    }
}

@Composable
fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onDownloadClick: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.add_download_task),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.paste_video_link_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                trailingIcon = {
                    if (url.isNotBlank()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.start_download))
            }
        }
    }
}

@Composable
fun LocalVideoInputCard(onPickVideo: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.local_video_upload),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onPickVideo,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.select_local_video_and_summarize))
            }
        }
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit,
    onOpenFolder: () -> Unit,
    onSummarize: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = task.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                TaskStatusBadge(status = task.progress.status)
            }

            if (task.progress.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    LinearProgressIndicator(
                        progress = { task.progress.progress / 100f },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${task.progress.progress.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${task.progress.currentSpeed} · ${task.progress.eta}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = task.summary.status != SummaryStatus.IDLE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryStatusBadge(status = task.summary.status)
                    val summaryText = stringResource(R.string.summary)
                    if (task.summary.summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            val context = LocalContext.current
                            val scope = rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        com.example.common.utils.PdfExporter.exportToPdf(context, task.title, task.summary.summary)
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.summary.summary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (task.progress.status == DownloadStatus.DOWNLOADING && task.id != "model_download_task") {
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.cancel))
                    }
                }

                if (task.progress.status == DownloadStatus.COMPLETED) {
                    if (task.id != "model_download_task") {
                        TextButton(onClick = onPlay) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.play_online))
                        }
                        TextButton(onClick = onOpenFolder) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.open_folder))
                        }
                        TextButton(onClick = onSummarize) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (task.summary.status == SummaryStatus.COMPLETED) {
                                    stringResource(
                                        R.string.re_summarize,
                                    )
                                } else {
                                    stringResource(R.string.generate_summary)
                                },
                            )
                        }
                    }
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remove))
                    }
                }

                if (task.progress.status == DownloadStatus.FAILED ||
                    task.progress.status == DownloadStatus.CANCELLED
                ) {
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remove))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskStatusBadge(status: DownloadStatus) {
    val (color, text) =
        when (status) {
            DownloadStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to stringResource(R.string.waiting)
            DownloadStatus.PREPARING -> MaterialTheme.colorScheme.primary to stringResource(R.string.preparing)
            DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary to stringResource(R.string.downloading)
            DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary to stringResource(R.string.completed)
            DownloadStatus.FAILED -> MaterialTheme.colorScheme.error to stringResource(R.string.failed)
            DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.secondary to stringResource(R.string.cancelled)
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SummaryStatusBadge(status: SummaryStatus) {
    val (color, text) =
        when (status) {
            SummaryStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to stringResource(R.string.not_started)
            SummaryStatus.PREPARING -> MaterialTheme.colorScheme.primary to stringResource(R.string.preparing)
            SummaryStatus.TRANSCRIBING -> MaterialTheme.colorScheme.primary to stringResource(R.string.transcribing)
            SummaryStatus.SUMMARIZING -> MaterialTheme.colorScheme.primary to stringResource(R.string.summarizing)
            SummaryStatus.COMPLETED -> MaterialTheme.colorScheme.primary to stringResource(R.string.summary_completed)
            SummaryStatus.FAILED -> MaterialTheme.colorScheme.error to stringResource(R.string.summary_failed)
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
fun SuccessCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_download_tasks),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.paste_video_link_to_start_download),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
