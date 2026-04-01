package com.example.summarizer.text_summarizer.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.SummaryHistoryDao
import com.example.common.database.models.SummaryHistoryEntity
import com.example.common.utils.NetworkMonitor
import com.example.common.utils.toUserFriendlyMessage
import com.example.summarizer.text_summarizer.services.TextExtractionService
import com.example.summarizer.text_summarizer.services.TextSummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextSummaryUiState(
    val inputText: String = "",
    val isSummarizing: Boolean = false,
    val isExtractingFile: Boolean = false,
    val summaryResult: String = "",
    val extractedFileName: String = "",
    val historyList: List<SummaryHistoryEntity> = emptyList(),
)

@HiltViewModel
class TextSummaryViewModel
    @Inject
    constructor(
        private val repository: TextSummaryRepository,
        private val textExtractionService: TextExtractionService,
        private val globalConfigRepository: GlobalConfigRepository,
        private val networkMonitor: NetworkMonitor,
        private val summaryHistoryDao: SummaryHistoryDao,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TextSummaryUiState())
        val uiState: StateFlow<TextSummaryUiState> = _uiState.asStateFlow()

        private val _errorEvents = kotlinx.coroutines.channels.Channel<String>()
        val errorEvents = _errorEvents.receiveAsFlow()

        init {
            viewModelScope.launch {
                summaryHistoryDao.getHistoryByType("text").collect { history ->
                    _uiState.value = _uiState.value.copy(historyList = history)
                }
            }
        }

        fun updateInputText(text: String) {
            _uiState.value = _uiState.value.copy(inputText = text)
        }

        fun summarize() {
            val textToSummarize = _uiState.value.inputText.trim()
            if (textToSummarize.isBlank()) {
                viewModelScope.launch { _errorEvents.send("输入文本不能为空") }
                return
            }
            if (!networkMonitor.isConnected.value) {
                viewModelScope.launch { _errorEvents.send("当前处于无网络环境，大模型总结服务暂不可用。\n您可以继续使用文件解析功能提取文本内容。") }
                return
            }

            _uiState.value = _uiState.value.copy(isSummarizing = true, summaryResult = "")

            viewModelScope.launch {
                try {
                    // Using Video Summary Configs for now as it represents the summarizer module configs
                    val apiKey = globalConfigRepository.getEffectiveVideoSummaryApiKey().first()
                    val baseUrl = globalConfigRepository.getVideoSummaryBaseUrl().first()
                    val modelName = globalConfigRepository.getVideoSummaryModelName().first()

                    val result = repository.summarizeText(apiKey, textToSummarize, modelName, baseUrl)

                    result.fold(
                        onSuccess = { summary ->
                            _uiState.value =
                                _uiState.value.copy(
                                    isSummarizing = false,
                                    summaryResult = summary,
                                )
                            viewModelScope.launch {
                                val title = if (_uiState.value.extractedFileName.isNotBlank()) {
                                    _uiState.value.extractedFileName
                                } else {
                                    textToSummarize.take(20).replace("\n", " ") + "..."
                                }
                                summaryHistoryDao.insertHistory(
                                    SummaryHistoryEntity(
                                        type = "text",
                                        sourceTitle = title,
                                        summaryResult = summary
                                    )
                                )
                            }
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
                            isSummarizing = false,
                        )
                    _errorEvents.send(e.toUserFriendlyMessage())
                }
            }
        }

        fun handleFileUri(uri: Uri) {
            _uiState.value = _uiState.value.copy(isExtractingFile = true)
            viewModelScope.launch {
                val result = textExtractionService.extractTextFromUri(uri)
                result.fold(
                    onSuccess = { extractedText ->
                        _uiState.value =
                            _uiState.value.copy(
                                isExtractingFile = false,
                                inputText = extractedText,
                            )
                    },
                    onFailure = { e ->
                        _uiState.value =
                            _uiState.value.copy(
                                isExtractingFile = false,
                            )
                        _errorEvents.send(e.message ?: "解析文件失败")
                    },
                )
            }
        }

        fun loadHistory(history: SummaryHistoryEntity) {
            _uiState.value = _uiState.value.copy(
                inputText = history.sourceTitle,
                summaryResult = history.summaryResult
            )
        }

        fun deleteHistory(history: SummaryHistoryEntity) {
            viewModelScope.launch {
                summaryHistoryDao.deleteHistory(history)
            }
        }
    }
