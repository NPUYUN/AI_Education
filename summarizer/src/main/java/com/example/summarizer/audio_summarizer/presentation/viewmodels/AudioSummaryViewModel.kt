package com.example.summarizer.audio_summarizer.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.GlobalConfigRepository
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.utils.NetworkMonitor
import com.example.common.utils.toUserFriendlyMessage
import com.example.summarizer.audio_summarizer.services.AudioSummaryRepository
import com.example.summarizer.videosummarizer.services.SherpaAsrManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class AudioSummaryUiState(
    val selectedAudioUri: Uri? = null,
    val selectedAudioName: String = "",
    val isTranscribing: Boolean = false,
    val isSummarizing: Boolean = false,
    val transcriptResult: String = "",
    val summaryResult: String = "",
)

@HiltViewModel
class AudioSummaryViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: AudioSummaryRepository,
        private val sherpaAsrManager: SherpaAsrManager,
        private val globalConfigRepository: GlobalConfigRepository,
        private val dispatcherProvider: DispatcherProvider,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AudioSummaryUiState())
        val uiState: StateFlow<AudioSummaryUiState> = _uiState.asStateFlow()

        private val _errorEvents = kotlinx.coroutines.channels.Channel<String>()
        val errorEvents = _errorEvents.receiveAsFlow()

        fun handleAudioUri(uri: Uri) {
            val name = getFileName(uri)
            _uiState.value =
                _uiState.value.copy(
                    selectedAudioUri = uri,
                    selectedAudioName = name,
                    transcriptResult = "",
                    summaryResult = "",
                )
        }

        private fun getFileName(uri: Uri): String {
            var name = "unknown_audio"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            return name
        }

        fun processAudio() {
            val uri = _uiState.value.selectedAudioUri
            if (uri == null) {
                viewModelScope.launch { _errorEvents.send("请先选择一个音频文件") }
                return
            }

            _uiState.value =
                _uiState.value.copy(
                    isTranscribing = true,
                    transcriptResult = "",
                    summaryResult = "",
                )

            viewModelScope.launch {
                try {
                    // 1. Copy URI to a temporary file
                    val tempFile =
                        withContext(dispatcherProvider.io) {
                            val file = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.tmp")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(file).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            file
                        }

                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        throw Exception("无法读取音频文件内容")
                    }

                    // 2. Transcribe using SherpaAsrManager
                    val transcript =
                        try {
                            sherpaAsrManager.transcribe(tempFile)
                        } finally {
                            withContext(dispatcherProvider.io) {
                                if (tempFile.exists()) tempFile.delete()
                            }
                        }

                    if (transcript.isBlank()) {
                        throw Exception("音频转写结果为空")
                    }

                    _uiState.value =
                        _uiState.value.copy(
                            isTranscribing = false,
                            transcriptResult = transcript,
                            isSummarizing = true,
                        )

                    if (!networkMonitor.isConnected.value) {
                        _uiState.value =
                            _uiState.value.copy(
                                isSummarizing = false,
                            )
                        _errorEvents.send("当前处于无网络环境，离线转写已完成，但无法进行大模型总结。\n您可以复制转写结果，待网络恢复后重试。")
                        return@launch
                    }

                    // 3. Summarize using API
                    // Using Video Summary Configs for now as it represents the summarizer module configs
                    val apiKey = globalConfigRepository.getEffectiveVideoSummaryApiKey().first()
                    val baseUrl = globalConfigRepository.getVideoSummaryBaseUrl().first()
                    val modelName = globalConfigRepository.getVideoSummaryModelName().first()

                    val result = repository.summarizeAudioTranscript(apiKey, transcript, modelName, baseUrl)

                    result.fold(
                        onSuccess = { summary ->
                            _uiState.value =
                                _uiState.value.copy(
                                    isSummarizing = false,
                                    summaryResult = summary,
                                )
                        },
                        onFailure = { e ->
                            _uiState.value =
                                _uiState.value.copy(
                                    isSummarizing = false,
                                )
                            _errorEvents.send(e.toUserFriendlyMessage())
                        },
                    )
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isTranscribing = false,
                            isSummarizing = false,
                        )
                    _errorEvents.send(e.toUserFriendlyMessage())
                }
            }
        }
    }
