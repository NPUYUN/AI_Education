package com.example.summarizer.videosummarizer.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.SummaryHistoryDao
import com.example.common.database.models.SummaryHistoryEntity
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.utils.NetworkMonitor
import com.example.summarizer.videosummarizer.services.DownloadProgress
import com.example.summarizer.videosummarizer.services.DownloadStatus
import com.example.summarizer.videosummarizer.services.ModelDownloader
import com.example.summarizer.videosummarizer.services.ProcessLocalVideoUseCase
import com.example.summarizer.videosummarizer.services.SherpaAsrManager
import com.example.summarizer.videosummarizer.services.SummarizeVideoUseCase
import com.example.summarizer.videosummarizer.services.VideoDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class VideoUrlItem(
    val url: String,
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val platform: String = "",
)

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val progress: DownloadProgress = DownloadProgress(),
    val localPath: String? = null,
    val summary: SummaryState = SummaryState(),
)

enum class SummaryStatus {
    IDLE,
    PREPARING,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    FAILED,
}

data class SummaryState(
    val status: SummaryStatus = SummaryStatus.IDLE,
    val transcript: String = "",
    val summary: String = "",
)

data class VideoDownloadUiState(
    val inputUrl: String = "",
    val isLoading: Boolean = false,
    val apiKey: String = "",
    val modelName: String = AppConstants.DEFAULT_MODEL_NAME,
    val baseUrl: String = AppConstants.BASE_URL,
    val showApiSettings: Boolean = false,
    val currentUserId: String = "", // Added to verify cross-app user data access
    val downloadTasks: List<DownloadTask> = emptyList(),
    val historyList: List<SummaryHistoryEntity> = emptyList(),
)

@HiltViewModel
class VideoDownloadViewModel
    @Inject
    constructor(
        private val application: Application,
        private val downloader: VideoDownloader,
        private val sherpaAsrManager: SherpaAsrManager,
        private val globalConfigRepository: GlobalConfigRepository,
        private val modelDownloader: ModelDownloader,
        private val dispatcherProvider: DispatcherProvider,
        private val processLocalVideoUseCase: ProcessLocalVideoUseCase,
        private val summarizeVideoUseCase: SummarizeVideoUseCase,
        private val networkMonitor: NetworkMonitor,
        private val summaryHistoryDao: SummaryHistoryDao,
    ) : ViewModel() {
        private val apiKeyKey = "bailian_api_key"

        private val _uiState = MutableStateFlow(VideoDownloadUiState())
        val uiState: StateFlow<VideoDownloadUiState> = _uiState.asStateFlow()

        private val _errorEvents = kotlinx.coroutines.channels.Channel<String>()
        val errorEvents = _errorEvents.receiveAsFlow()

        private val _successEvents = kotlinx.coroutines.channels.Channel<String>()
        val successEvents = _successEvents.receiveAsFlow()

        private val cancelledTasks = mutableSetOf<String>()
        private val summarizingTaskIds = mutableSetOf<String>()

        init {
            // Verify cross-app user data access
            viewModelScope.launch(dispatcherProvider.main) {
                globalConfigRepository.getCurrentUserId().collect { userId ->
                    _uiState.value = _uiState.value.copy(currentUserId = userId)
                }
            }

            viewModelScope.launch(dispatcherProvider.main) {
                globalConfigRepository.getEffectiveVideoSummaryApiKey().collect { key ->
                    _uiState.value = _uiState.value.copy(apiKey = key)
                }
            }
            viewModelScope.launch(dispatcherProvider.main) {
                globalConfigRepository.getVideoSummaryModelName().collect { modelName ->
                    _uiState.value = _uiState.value.copy(modelName = modelName)
                }
            }
            viewModelScope.launch(dispatcherProvider.main) {
                globalConfigRepository.getVideoSummaryBaseUrl().collect { baseUrl ->
                    _uiState.value = _uiState.value.copy(baseUrl = baseUrl)
                }
            }
            viewModelScope.launch(dispatcherProvider.main) {
                summaryHistoryDao.getHistoryByType("video").collect { history ->
                    _uiState.value = _uiState.value.copy(historyList = history)
                }
            }
            checkAndDownloadModel()
        }

        private fun checkAndDownloadModel() {
            if (!modelDownloader.isModelReady()) {
                val taskId = "model_download_task"
                val modelTask =
                    DownloadTask(
                        id = taskId,
                        url = "https://github.com/k2-fsa/sherpa-onnx",
                        title = "离线语音识别模型 (首次运行必需)",
                        progress = DownloadProgress(status = DownloadStatus.PREPARING),
                    )
                _uiState.update { state ->
                    state.copy(downloadTasks = state.downloadTasks + modelTask)
                }

                viewModelScope.launch(dispatcherProvider.main) {
                    modelDownloader.downloadAndExtractModel { progress ->
                        updateTaskProgress(taskId) { progress }
                    }.fold(
                        onSuccess = { dir ->
                            updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f) }
                            updateTask(taskId) { it.copy(localPath = dir.absolutePath) }
                            showSuccess("语音识别模型下载完成！")
                        },
                        onFailure = { error ->
                            updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.FAILED) }
                            showError("模型下载失败: ${error.message}")
                        },
                    )
                }
            }
        }

        fun updateInputUrl(url: String) {
            _uiState.value = _uiState.value.copy(inputUrl = url)
        }

        fun setApiSettingsVisible(visible: Boolean) {
            // Update API settings visibility
            _uiState.value = _uiState.value.copy(showApiSettings = visible)
        }

        fun addDownloadTask(url: String) {
            if (!networkMonitor.isConnected.value) {
                showError("当前处于无网络环境，无法下载视频。")
                return
            }
            val normalizedUrl = normalizeBilibiliShareUrl(url.trim())
            val taskId = System.currentTimeMillis().toString()
            val newTask =
                DownloadTask(
                    id = taskId,
                    url = normalizedUrl,
                    title = extractVideoTitle(normalizedUrl),
                )
            _uiState.update { state ->
                state.copy(downloadTasks = state.downloadTasks + newTask)
            }
            startDownload(taskId, normalizedUrl)
            _uiState.value = _uiState.value.copy(inputUrl = "")
        }

        /** 与 [VideoDownloader] 一致：移动版 B 站链接统一为 www，减少解析失败 */
        private fun normalizeBilibiliShareUrl(url: String): String {
            if (!url.contains("bilibili.com", ignoreCase = true)) return url
            return url.replace("://m.bilibili.com", "://www.bilibili.com", ignoreCase = true)
        }

        fun handleLocalVideo(uri: android.net.Uri) {
            viewModelScope.launch(dispatcherProvider.io) {
                val result = processLocalVideoUseCase(application, uri)

                result.fold(
                    onSuccess = { (fileName, destFile) ->
                        val taskId = "local_${System.currentTimeMillis()}"
                        val newTask =
                            DownloadTask(
                                id = taskId,
                                url = "Local File: $fileName",
                                title = fileName,
                                progress = DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f),
                                localPath = destFile.absolutePath,
                            )

                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                state.copy(downloadTasks = state.downloadTasks + newTask)
                            }
                            showSuccess("视频导入成功")
                        }
                        // Auto start summary
                        startVideoSummary(taskId, destFile.absolutePath)
                    },
                    onFailure = { error ->
                        withContext(dispatcherProvider.main) {
                            showError("导入视频失败: ${error.message}")
                        }
                    },
                )
            }
        }

        private fun startDownload(
            taskId: String,
            url: String,
        ) {
            viewModelScope.launch(dispatcherProvider.io) {
                updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.DOWNLOADING) }

                downloader.downloadVideo(url) { progress ->
                    updateTaskProgress(taskId) { progress }
                }.fold(
                    onSuccess = { filePath ->
                        updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.COMPLETED) }
                        updateTask(taskId) { it.copy(localPath = filePath) }
                        showSuccess("下载完成：$filePath")
                        startVideoSummary(taskId, filePath)
                    },
                    onFailure = { error ->
                        if (cancelledTasks.contains(taskId)) {
                            cancelledTasks.remove(taskId)
                            updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.CANCELLED) }
                        } else {
                            updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.FAILED) }
                            showError(humanizeDownloadError(error))
                        }
                    },
                )
            }
        }

        fun cancelDownload(taskId: String) {
            if (taskId == "model_download_task") {
                return // Not supported for model download yet
            }
            cancelledTasks.add(taskId)
            downloader.cancelDownload()
            updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.CANCELLED) }
        }

        fun removeTask(taskId: String) {
            _uiState.update { state ->
                state.copy(downloadTasks = state.downloadTasks.filter { it.id != taskId })
            }
        }

        fun clearError() {
            // Deprecated
        }

        fun clearSuccess() {
            // Deprecated
        }

        fun startVideoSummary(
            taskId: String,
            localPath: String?,
        ) {
            val task = _uiState.value.downloadTasks.firstOrNull { it.id == taskId }
            if (task == null) {
                showError("任务不存在或已被移除")
                return
            }

            val status = task.summary.status
            if (status == SummaryStatus.PREPARING ||
                status == SummaryStatus.TRANSCRIBING ||
                status == SummaryStatus.SUMMARIZING
            ) return

            synchronized(summarizingTaskIds) {
                if (summarizingTaskIds.contains(taskId)) return
                summarizingTaskIds.add(taskId)
            }

            val resolvedPath = localPath?.takeIf { it.isNotBlank() } ?: task.localPath
            val localFile = resolvedPath?.let { File(it) }
            if (localFile == null || !localFile.exists()) {
                synchronized(summarizingTaskIds) { summarizingTaskIds.remove(taskId) }
                updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED) }
                showError("未找到本地文件，无法生成摘要")
                return
            }

            if (!modelDownloader.isModelReady()) {
                synchronized(summarizingTaskIds) { summarizingTaskIds.remove(taskId) }
                showError("语音识别模型尚未就绪，请等待模型下载完成")
                return
            }

            // Immediately mark as preparing to prevent rapid double-click concurrent jobs.
            updateTaskSummary(taskId) {
                it.copy(
                    status = SummaryStatus.PREPARING,
                    transcript = "",
                    summary = "",
                )
            }

            viewModelScope.launch(dispatcherProvider.io) {
                try {
                    if (!networkMonitor.isConnected.value) {
                        val errorMsg = "当前处于无网络环境，无法使用大模型总结视频。\n您可以继续使用本地视频提取音频及离线转写功能。"
                        updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED) }
                        showError(errorMsg)
                        return@launch
                    }

                    val apiKey = _uiState.value.apiKey.trim()
                    val modelName = _uiState.value.modelName
                    val baseUrl = _uiState.value.baseUrl

                    if (apiKey.isBlank()) {
                        updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED) }
                        showError("尚未配置 API Key，请在弹出的设置中进行配置。")
                        _uiState.value = _uiState.value.copy(showApiSettings = true)
                        return@launch
                    }

                    updateTaskSummary(taskId) { it.copy(status = SummaryStatus.TRANSCRIBING) }

                    val result =
                        summarizeVideoUseCase(
                            apiKey = apiKey,
                            localFile = localFile,
                            modelName = modelName,
                            baseUrl = baseUrl,
                            onTranscriptReady = { transcript ->
                                updateTaskSummary(taskId) { it.copy(status = SummaryStatus.SUMMARIZING, transcript = transcript) }
                            },
                        )

                    result.fold(
                        onSuccess = { summaryText ->
                            updateTaskSummary(taskId) { it.copy(status = SummaryStatus.COMPLETED, summary = summaryText) }
                            showSuccess("视频总结已完成！")

                            // Save to history
                            viewModelScope.launch(dispatcherProvider.io) {
                                try {
                                    val task = _uiState.value.downloadTasks.firstOrNull { it.id == taskId }
                                    val title = task?.title ?: "Video Summary"
                                    summaryHistoryDao.insertHistory(
                                        SummaryHistoryEntity(
                                            type = "video",
                                            sourceTitle = title,
                                            summaryResult = summaryText,
                                        ),
                                    )
                                } catch (e: Exception) {
                                    // ignore history insertion failure
                                }
                            }
                        },
                        onFailure = { error ->
                            val message = error.message ?: "处理失败"
                            updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED) }
                            showError(message)
                            if (message.contains("API Key 无效或未授权") || message.contains("尚未配置 API Key")) {
                                _uiState.value = _uiState.value.copy(showApiSettings = true)
                            }
                        },
                    )
                } catch (e: Throwable) {
                    updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED) }
                    showError("发生未知错误: ${e.message}")
                } finally {
                    synchronized(summarizingTaskIds) { summarizingTaskIds.remove(taskId) }
                }
            }
        }

        private fun updateTaskProgress(
            taskId: String,
            update: (DownloadProgress) -> DownloadProgress,
        ) {
            _uiState.update { state ->
                state.copy(
                    downloadTasks =
                        state.downloadTasks.map { task ->
                            if (task.id == taskId) task.copy(progress = update(task.progress)) else task
                        },
                )
            }
        }

        private fun updateTask(
            taskId: String,
            update: (DownloadTask) -> DownloadTask,
        ) {
            _uiState.update { state ->
                state.copy(
                    downloadTasks =
                        state.downloadTasks.map { task ->
                            if (task.id == taskId) update(task) else task
                        },
                )
            }
        }

        private fun updateTaskSummary(
            taskId: String,
            update: (SummaryState) -> SummaryState,
        ) {
            _uiState.update { state ->
                state.copy(
                    downloadTasks =
                        state.downloadTasks.map { task ->
                            if (task.id == taskId) task.copy(summary = update(task.summary)) else task
                        },
                )
            }
        }

        private fun showError(message: String) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            viewModelScope.launch {
                _errorEvents.send(message)
            }
        }

        private fun showSuccess(message: String) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            viewModelScope.launch {
                _successEvents.send(message)
            }
        }

        private fun extractVideoTitle(url: String): String {
            return when {
                url.contains("bilibili.com") || url.contains("b23.tv") -> "哔哩哔哩视频"
                url.contains("youtube.com") || url.contains("youtu.be") -> "YouTube 视频"
                url.contains("douyin.com") -> "抖音视频"
                url.contains("tiktok.com") -> "TikTok 视频"
                url.contains("xiaohongshu.com") -> "小红书视频"
                else -> "视频下载任务"
            }
        }

        private fun humanizeDownloadError(error: Throwable): String {
            val raw = error.message?.trim().orEmpty()
            val extracted = extractErrorLine(raw)
            val normalized = extracted.lowercase()

            val message =
                when {
                    normalized.contains("cannot link executable") ||
                        normalized.contains("em_aarch64") ||
                        normalized.contains("em_x86_64") ||
                        (normalized.contains("libz.so") && normalized.contains("instead of")) ->
                        "下载失败：当前设备 CPU 架构与已解压的下载组件不匹配（常见于 x86 模拟器仅打包 arm64）。请重新安装应用或使用 arm64 系统镜像模拟器；若仍失败，请清除应用数据后重试。"
                    normalized.contains("ssl") || normalized.contains("certificate") ->
                        "下载失败：HTTPS 连接异常（SSL/TLS），已启用更友好的连接参数并忽略证书校验，请重试。"
                    normalized.contains("nonetype") && normalized.contains("lower") ->
                        "下载失败：链接解析异常（可能是短链接或平台规则更新），已建议尝试更新解析器后重试。"
                    normalized.contains("no supported javascript runtime") || normalized.contains("ejs") ->
                        "下载失败：YouTube 解析需要 JS 运行时（EJS），可尝试切换提取参数或更新解析器。"
                    normalized.contains("requested format is not available") ->
                        "下载失败：该视频可用清晰度/格式与当前策略不匹配，已调整格式选择策略后请重试。"
                    normalized.contains("unsupported url") || normalized.contains("no suitable extractor") ->
                        "下载失败：不支持的链接或平台。"
                    normalized.contains("network") || normalized.contains("timeout") || normalized.contains("unable to resolve host") ->
                        "下载失败：网络连接异常，请检查网络后重试。"
                    extracted.isNotBlank() -> "下载失败：$extracted"
                    else -> "下载失败：未知错误"
                }

            return message
        }

        private fun extractErrorLine(raw: String): String {
            if (raw.isBlank()) return ""
            val idx = raw.lastIndexOf("ERROR:")
            val candidate = if (idx >= 0) raw.substring(idx).trim() else raw.trim()
            return candidate.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filterNot { it.startsWith("WARNING:", ignoreCase = true) }
                .take(4)
                .joinToString("\n")
        }

        fun deleteHistory(history: SummaryHistoryEntity) {
            viewModelScope.launch(dispatcherProvider.io) {
                summaryHistoryDao.deleteHistory(history)
            }
        }
    }
