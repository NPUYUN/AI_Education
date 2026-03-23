package com.example.summarizer.text_summarizer.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.GlobalConfigRepository
import com.example.summarizer.text_summarizer.services.TextExtractionService
import com.example.summarizer.text_summarizer.services.TextSummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextSummaryUiState(
    val inputText: String = "",
    val isSummarizing: Boolean = false,
    val isExtractingFile: Boolean = false,
    val summaryResult: String = "",
    val error: String? = null
)

@HiltViewModel
class TextSummaryViewModel @Inject constructor(
    private val repository: TextSummaryRepository,
    private val textExtractionService: TextExtractionService,
    private val globalConfigRepository: GlobalConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextSummaryUiState())
    val uiState: StateFlow<TextSummaryUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, error = null)
    }

    fun summarize() {
        val textToSummarize = _uiState.value.inputText.trim()
        if (textToSummarize.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "输入文本不能为空")
            return
        }

        _uiState.value = _uiState.value.copy(isSummarizing = true, error = null, summaryResult = "")

        viewModelScope.launch {
            try {
                // Using Video Summary Configs for now as it represents the summarizer module configs
                val apiKey = globalConfigRepository.getEffectiveVideoSummaryApiKey().first()
                val baseUrl = globalConfigRepository.getVideoSummaryBaseUrl().first()
                val modelName = globalConfigRepository.getVideoSummaryModelName().first()

                val result = repository.summarizeText(apiKey, textToSummarize, modelName, baseUrl)

                result.fold(
                    onSuccess = { summary ->
                        _uiState.value = _uiState.value.copy(
                            isSummarizing = false,
                            summaryResult = summary
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isSummarizing = false,
                            error = e.message ?: "生成总结时发生未知错误"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSummarizing = false,
                    error = e.message ?: "发生异常"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun handleFileUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(isExtractingFile = true, error = null)
        viewModelScope.launch {
            val result = textExtractionService.extractTextFromUri(uri)
            result.fold(
                onSuccess = { extractedText ->
                    _uiState.value = _uiState.value.copy(
                        isExtractingFile = false,
                        inputText = extractedText
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isExtractingFile = false,
                        error = e.message ?: "解析文件失败"
                    )
                }
            )
        }
    }
}
