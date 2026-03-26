package com.example.summarizer.dialogue_summarizer.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ChatDao
import com.example.common.database.models.ChatSessionEntity
import com.example.common.utils.NetworkMonitor
import com.example.summarizer.dialogue_summarizer.services.DialogueSummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DialogueSummaryUiState(
    val sessions: List<ChatSessionEntity> = emptyList(),
    val selectedSession: ChatSessionEntity? = null,
    val isSummarizing: Boolean = false,
    val summaryResult: String = "",
    val error: String? = null,
)

@HiltViewModel
class DialogueSummaryViewModel
    @Inject
    constructor(
        private val repository: DialogueSummaryRepository,
        private val chatDao: ChatDao,
        private val globalConfigRepository: GlobalConfigRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DialogueSummaryUiState())
        val uiState: StateFlow<DialogueSummaryUiState> = _uiState.asStateFlow()

        init {
            loadSessions()
        }

        private fun loadSessions() {
            viewModelScope.launch {
                // Assuming "default_user" for now as used elsewhere
                chatDao.getSessions("default_user")
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(error = "加载会话记录失败: ${e.message}")
                    }
                    .collect { sessions ->
                        _uiState.value = _uiState.value.copy(sessions = sessions)
                    }
            }
        }

        fun selectSession(session: ChatSessionEntity) {
            _uiState.value =
                _uiState.value.copy(
                    selectedSession = session,
                    summaryResult = "",
                    error = null,
                )
        }

        fun summarizeSelectedSession() {
            val session = _uiState.value.selectedSession
            if (session == null) {
                _uiState.value = _uiState.value.copy(error = "请先选择一个对话")
                return
            }

            if (!networkMonitor.isConnected.value) {
                _uiState.value = _uiState.value.copy(error = "当前处于无网络环境，大模型总结服务暂不可用。\n您可以继续查看本地历史对话列表。")
                return
            }

            _uiState.value = _uiState.value.copy(isSummarizing = true, error = null, summaryResult = "")

            viewModelScope.launch {
                try {
                    val apiKey =
                        globalConfigRepository.getAiTutorApiKey().firstOrNull()
                            ?.takeIf { it.isNotBlank() } ?: com.example.common.config.AppConstants.DEFAULT_API_KEY
                    val modelName =
                        globalConfigRepository.getAiTutorModelName().firstOrNull()
                            ?: com.example.common.config.AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl =
                        globalConfigRepository.getAiTutorBaseUrl().firstOrNull()
                            ?: com.example.common.config.AppConstants.BASE_URL

                    val messages = chatDao.getMessages(session.id).firstOrNull() ?: emptyList()

                    if (messages.isEmpty()) {
                        _uiState.value =
                            _uiState.value.copy(
                                isSummarizing = false,
                                error = "该对话没有内容",
                            )
                        return@launch
                    }

                    val result = repository.summarizeDialogue(apiKey, messages, modelName, baseUrl)

                    if (result.isSuccess) {
                        _uiState.value =
                            _uiState.value.copy(
                                isSummarizing = false,
                                summaryResult = result.getOrNull() ?: "",
                            )
                    } else {
                        _uiState.value =
                            _uiState.value.copy(
                                isSummarizing = false,
                                error = result.exceptionOrNull()?.message ?: "未知错误",
                            )
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isSummarizing = false,
                            error = e.localizedMessage ?: "处理失败",
                        )
                }
            }
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }
