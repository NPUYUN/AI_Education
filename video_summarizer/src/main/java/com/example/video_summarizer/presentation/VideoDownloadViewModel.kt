package com.example.video_summarizer.presentation

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.database.PreferencesManager
import com.example.video_summarizer.data.asr.SherpaAsrManager
import com.example.video_summarizer.data.downloader.DownloadProgress
import com.example.video_summarizer.data.downloader.DownloadStatus
import com.example.video_summarizer.data.downloader.VideoDownloader
import com.example.video_summarizer.data.summary.BailianSummaryRepository
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

import com.example.video_summarizer.data.downloader.ModelDownloader
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
    val currentUserId: String = "" // Added to verify cross-app user data access
)

@HiltViewModel
class VideoDownloadViewModel @Inject constructor(
    private val application: Application,
    private val downloader: VideoDownloader,
    private val sherpaAsrManager: SherpaAsrManager,
    private val preferences: PreferencesManager,
    private val modelDownloader: ModelDownloader,
    private val summaryRepository: BailianSummaryRepository,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val apiKeyKey = "bailian_api_key"

    private val _downloadTasks = androidx.compose.runtime.mutableStateListOf<DownloadTask>()
    val downloadTasks: List<DownloadTask> get() = _downloadTasks

    private val _uiState = MutableStateFlow(VideoDownloadUiState())
    val uiState: StateFlow<VideoDownloadUiState> = _uiState.asStateFlow()

    init {
        // Verify cross-app user data access
        viewModelScope.launch(dispatcherProvider.main) {
            preferences.getString("current_user_id").collect { userId ->
                _uiState.value = _uiState.value.copy(currentUserId = userId)
            }
        }
        
        viewModelScope.launch(dispatcherProvider.main) {
            preferences.getString("api_key_video_summary", "").collect { key ->
                val finalKey = key.ifBlank {
                    preferences.getString("bailian_api_key", "").first()
                }
                _uiState.value = _uiState.value.copy(apiKey = finalKey)
            }
        }
        viewModelScope.launch(dispatcherProvider.main) {
            preferences.getString("model_name_video_summary", AppConstants.DEFAULT_MODEL_NAME).collect { modelName ->
                _uiState.value = _uiState.value.copy(modelName = modelName)
            }
        }
        viewModelScope.launch(dispatcherProvider.main) {
            preferences.getString("base_url_video_summary", AppConstants.BASE_URL).collect { baseUrl ->
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
            _downloadTasks.add(modelTask)

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
        _downloadTasks.add(newTask)
        startDownload(taskId, url)
        _uiState.value = _uiState.value.copy(inputUrl = "")
    }

    fun handleLocalVideo(uri: android.net.Uri) {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val context = application
                val contentResolver = context.contentResolver
                
                // Get file name
                var fileName = "local_video_${System.currentTimeMillis()}.mp4"
                contentResolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            cursor.getString(nameIndex)?.let { 
                                fileName = it 
                            }
                        }
                    }
                }
                
                // Copy to private storage
                val destFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), fileName)
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val taskId = "local_${System.currentTimeMillis()}"
                val newTask = DownloadTask(
                    id = taskId,
                    url = "Local File: $fileName",
                    title = fileName,
                    progress = DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f),
                    localPath = destFile.absolutePath
                )
                
                withContext(dispatcherProvider.main) {
                    _downloadTasks.add(newTask)
                    showSuccess("视频导入成功")
                }
                // Auto start summary
                startVideoSummary(taskId, destFile.absolutePath)
                
            } catch (e: Exception) {
                withContext(dispatcherProvider.main) {
                    showError("导入视频失败: ${e.message}")
                }
            }
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
        val index = _downloadTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            _downloadTasks.removeAt(index)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun startVideoSummary(taskId: String, localPath: String?) {
        val status = _downloadTasks.firstOrNull { it.id == taskId }?.summary?.status
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
            if (localFile == null || !localFile.exists()) {
                updateTaskSummary(taskId) {
                    it.copy(
                        status = SummaryStatus.FAILED,
                        error = "未找到本地文件，无法上传转写"
                    )
                }
                showError("未找到本地文件，无法上传转写")
                return@launch
            }

            updateTaskSummary(taskId) { it.copy(status = SummaryStatus.TRANSCRIBING) }
            val transcriptResult = summaryRepository.transcribeOffline(localFile)
            val transcript = transcriptResult.getOrNull().orEmpty()
            if (transcript.isBlank()) {
                val message = transcriptResult.exceptionOrNull()?.message ?: "转写失败"
                updateTaskSummary(taskId) {
                    it.copy(
                        status = SummaryStatus.FAILED,
                        error = message
                    )
                }
                showError(message)
                return@launch
            }

            updateTaskSummary(taskId) { it.copy(status = SummaryStatus.SUMMARIZING, transcript = transcript) }
            val summaryResult = summaryRepository.summarize(apiKey, transcript, modelName, baseUrl)
            val summaryText = summaryResult.getOrNull().orEmpty()
            if (summaryText.isBlank()) {
                val message = summaryResult.exceptionOrNull()?.message ?: "摘要生成失败"
                updateTaskSummary(taskId) {
                    it.copy(
                        status = SummaryStatus.FAILED,
                        error = message
                    )
                }
                showError(message)
                if (message.contains("API Key 无效或未授权") || message.contains("尚未配置 API Key")) {
                    _uiState.value = _uiState.value.copy(showApiSettings = true)
                }
                return@launch
            }

            updateTaskSummary(taskId) { it.copy(status = SummaryStatus.COMPLETED, summary = summaryText) }
        }
    }

    private fun updateTaskProgress(taskId: String, update: (DownloadProgress) -> DownloadProgress) {
        val index = _downloadTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = _downloadTasks[index]
            _downloadTasks[index] = task.copy(progress = update(task.progress))
        }
    }

    private fun updateTask(taskId: String, update: (DownloadTask) -> DownloadTask) {
        val index = _downloadTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            _downloadTasks[index] = update(_downloadTasks[index])
        }
    }

    private fun updateTaskSummary(taskId: String, update: (SummaryState) -> SummaryState) {
        val index = _downloadTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = _downloadTasks[index]
            _downloadTasks[index] = task.copy(summary = update(task.summary))
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
