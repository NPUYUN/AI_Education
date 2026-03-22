package com.example.summarizer.video_summarizer.presentation.viewmodels

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.summarizer.video_summarizer.services.SherpaAsrManager
import com.example.summarizer.video_summarizer.services.DownloadProgress
import com.example.summarizer.video_summarizer.services.DownloadStatus
import com.example.summarizer.video_summarizer.services.VideoDownloader
import com.example.summarizer.video_summarizer.services.BailianSummaryRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback

import com.example.summarizer.video_summarizer.services.ProcessLocalVideoUseCase
import com.example.summarizer.video_summarizer.services.SummarizeVideoUseCase
import com.example.summarizer.video_summarizer.services.ModelDownloader
import com.example.common.dispatchers.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class VideoUrlItem(
    val url: String,
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val platform: String = ""
)

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val progress: DownloadProgress = DownloadProgress(),
    val localPath: String? = null,
    val summary: SummaryState = SummaryState()
)

enum class SummaryStatus {
    IDLE,
    PREPARING,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    FAILED
}

data class SummaryState(
    val status: SummaryStatus = SummaryStatus.IDLE,
    val transcript: String = "",
    val summary: String = "",
    val error: String? = null
)

data class VideoDownloadUiState(
    val inputUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val apiKey: String = "",
    val modelName: String = AppConstants.DEFAULT_MODEL_NAME,
    val baseUrl: String = AppConstants.BASE_URL,
    val showApiSettings: Boolean = false,
    val currentUserId: String = "", // Added to verify cross-app user data access
    val downloadTasks: List<DownloadTask> = emptyList()
)

@HiltViewModel
class VideoDownloadViewModel @Inject constructor(
    private val application: Application,
    private val downloader: VideoDownloader,
    private val sherpaAsrManager: SherpaAsrManager,
    private val globalConfigRepository: GlobalConfigRepository,
    private val modelDownloader: ModelDownloader,
    private val dispatcherProvider: DispatcherProvider,
    private val processLocalVideoUseCase: ProcessLocalVideoUseCase,
    private val summarizeVideoUseCase: SummarizeVideoUseCase
) : ViewModel() {

    private val apiKeyKey = "bailian_api_key"

    private val _uiState = MutableStateFlow(VideoDownloadUiState())
    val uiState: StateFlow<VideoDownloadUiState> = _uiState.asStateFlow()

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
        checkAndDownloadModel()
    }

    private fun checkAndDownloadModel() {
        if (!modelDownloader.isModelReady()) {
            val taskId = "model_download_task"
            val modelTask = DownloadTask(
                id = taskId,
                url = "https://github.com/k2-fsa/sherpa-onnx",
                title = "离线语音识别模型 (首次运行必需)",
                progress = DownloadProgress(status = DownloadStatus.PREPARING)
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
                    }
                )
            }
        }
    }

    fun updateInputUrl(url: String) {
        _uiState.value = _uiState.value.copy(inputUrl = url, error = null)
    }

    fun setApiSettingsVisible(visible: Boolean) {
        // Update API settings visibility
        _uiState.value = _uiState.value.copy(showApiSettings = visible)
    }

    fun addDownloadTask(url: String) {
        val taskId = System.currentTimeMillis().toString()
        val newTask = DownloadTask(
            id = taskId,
            url = url,
            title = extractVideoTitle(url)
        )
        _uiState.update { state -> 
            state.copy(downloadTasks = state.downloadTasks + newTask)
        }
        startDownload(taskId, url)
        _uiState.value = _uiState.value.copy(inputUrl = "")
    }

    fun handleLocalVideo(uri: android.net.Uri) {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = processLocalVideoUseCase(application, uri)
            
            result.fold(
                onSuccess = { (fileName, destFile) ->
                    val taskId = "local_${System.currentTimeMillis()}"
                    val newTask = DownloadTask(
                        id = taskId,
                        url = "Local File: $fileName",
                        title = fileName,
                        progress = DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f),
                        localPath = destFile.absolutePath
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
                }
            )
        }
    }

   private fun startDownload(taskId: String, url: String) {
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
                    updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.FAILED) }
                    showError(humanizeDownloadError(error))
                }
            )
        }
    }

    fun cancelDownload(taskId: String) {
        if (taskId == "model_download_task") {
            return // Not supported for model download yet
        }
        downloader.cancelDownload()
        updateTaskProgress(taskId) { DownloadProgress(status = DownloadStatus.CANCELLED) }
    }

    fun removeTask(taskId: String) {
        _uiState.update { state ->
            state.copy(downloadTasks = state.downloadTasks.filter { it.id != taskId })
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun startVideoSummary(taskId: String, localPath: String?) {
        val status = _uiState.value.downloadTasks.firstOrNull { it.id == taskId }?.summary?.status
        if (status == SummaryStatus.PREPARING ||
            status == SummaryStatus.TRANSCRIBING ||
            status == SummaryStatus.SUMMARIZING) {
            return
        }
        if (!modelDownloader.isModelReady()) {
            showError("语音识别模型尚未就绪，请等待模型下载完成")
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            val apiKey = _uiState.value.apiKey.trim()
            val modelName = _uiState.value.modelName
            val baseUrl = _uiState.value.baseUrl

            if (apiKey.isBlank()) {
                updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED, error = "尚未配置 API Key，请在弹出的设置中进行配置。") }
                showError("尚未配置 API Key，请在弹出的设置中进行配置。")
                _uiState.value = _uiState.value.copy(showApiSettings = true)
                return@launch
            }

            updateTaskSummary(taskId) {
                it.copy(
                    status = SummaryStatus.PREPARING,
                    transcript = "",
                    summary = "",
                    error = null
                )
            }

            val localFile = localPath?.let { File(it) }
            if (localFile == null) {
                val errorMsg = "未找到本地文件，无法上传转写"
                updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED, error = errorMsg) }
                showError(errorMsg)
                return@launch
            }

            updateTaskSummary(taskId) { it.copy(status = SummaryStatus.TRANSCRIBING) }
            
            val result = summarizeVideoUseCase(
                apiKey = apiKey,
                localFile = localFile,
                modelName = modelName,
                baseUrl = baseUrl,
                onTranscriptReady = { transcript ->
                    updateTaskSummary(taskId) { it.copy(status = SummaryStatus.SUMMARIZING, transcript = transcript) }
                }
            )

            result.fold(
                onSuccess = { summaryText ->
                    updateTaskSummary(taskId) { it.copy(status = SummaryStatus.COMPLETED, summary = summaryText) }
                },
                onFailure = { error ->
                    val message = error.message ?: "处理失败"
                    updateTaskSummary(taskId) { it.copy(status = SummaryStatus.FAILED, error = message) }
                    showError(message)
                    if (message.contains("API Key 无效或未授权") || message.contains("尚未配置 API Key")) {
                        _uiState.value = _uiState.value.copy(showApiSettings = true)
                    }
                }
            )
        }
    }

    private fun updateTaskProgress(taskId: String, update: (DownloadProgress) -> DownloadProgress) {
        _uiState.update { state ->
            state.copy(downloadTasks = state.downloadTasks.map { task ->
                if (task.id == taskId) task.copy(progress = update(task.progress)) else task
            })
        }
    }

    private fun updateTask(taskId: String, update: (DownloadTask) -> DownloadTask) {
        _uiState.update { state ->
            state.copy(downloadTasks = state.downloadTasks.map { task ->
                if (task.id == taskId) update(task) else task
            })
        }
    }

    private fun updateTaskSummary(taskId: String, update: (SummaryState) -> SummaryState) {
        _uiState.update { state ->
            state.copy(downloadTasks = state.downloadTasks.map { task ->
                if (task.id == taskId) task.copy(summary = update(task.summary)) else task
            })
        }
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(error = message, isLoading = false)
    }

    private fun showSuccess(message: String) {
        _uiState.value = _uiState.value.copy(successMessage = message, isLoading = false)
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

    override fun onCleared() {
        super.onCleared()
        // Release resources in a separate scope to ensure it completes even if ViewModel is cleared
        // and to avoid blocking the main thread
        kotlinx.coroutines.CoroutineScope(dispatcherProvider.io).launch {
            sherpaAsrManager.release()
        }
    }

    private fun humanizeDownloadError(error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        val extracted = extractErrorLine(raw)
        val normalized = extracted.lowercase()

        val message = when {
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
}
