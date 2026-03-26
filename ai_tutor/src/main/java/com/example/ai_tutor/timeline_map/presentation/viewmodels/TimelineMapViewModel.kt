package com.example.ai_tutor.timeline_map.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tutor.timeline_map.models.HistoricalEvent
import com.example.ai_tutor.timeline_map.models.SpeechLanguage
import com.example.ai_tutor.timeline_map.services.MockKnowledgeGraphManager
import com.example.ai_tutor.timeline_map.services.TimelineRepository
import com.example.ai_tutor.timeline_map.utils.DateUtils
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.manager.VoskVoiceManager
import com.example.common.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineMapUiState(
    val queryText: String = "",
    val speechLanguage: SpeechLanguage = SpeechLanguage.AUTO,
    val isListening: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val mapTileWarning: String? = null,
    val events: List<HistoricalEvent> = emptyList(),
    val selectedEventId: String? = null,
    val timelineZoom: Float = 1f,
    val apiKey: String = "",
    val modelName: String = AppConstants.DEFAULT_MODEL_NAME,
    val baseUrl: String = AppConstants.BASE_URL,
    val showApiSettings: Boolean = false,
)

@HiltViewModel
class TimelineMapViewModel
    @Inject
    constructor(
        private val globalConfigRepository: GlobalConfigRepository,
        private val voskVoiceManager: VoskVoiceManager,
        private val repository: TimelineRepository,
        private val dispatcherProvider: DispatcherProvider,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val knowledgeGraphManager = MockKnowledgeGraphManager()

        private val _uiState = MutableStateFlow(TimelineMapUiState())
        val uiState: StateFlow<TimelineMapUiState> = _uiState.asStateFlow()

        init {
            voskVoiceManager.init(viewModelScope)

            viewModelScope.launch(dispatcherProvider.main) {
                globalConfigRepository.getEffectiveTimelineMapApiKey().collectLatest { key ->
                    _uiState.update { it.copy(apiKey = key) }
                }
            }

            viewModelScope.launch(dispatcherProvider.io) {
                globalConfigRepository.getTimelineMapModelName().collectLatest { name ->
                    _uiState.update { it.copy(modelName = name) }
                }
            }

            viewModelScope.launch(dispatcherProvider.io) {
                globalConfigRepository.getTimelineMapBaseUrl().collectLatest { url ->
                    _uiState.update { it.copy(baseUrl = url) }
                }
            }

            viewModelScope.launch(dispatcherProvider.main) {
                voskVoiceManager.voiceState.collectLatest { state ->
                    when (state) {
                        is VoskVoiceManager.VoiceState.Listening -> _uiState.update { it.copy(isListening = true) }
                        is VoskVoiceManager.VoiceState.Result -> {
                            if (state.text.isNotEmpty()) {
                                _uiState.update { it.copy(queryText = state.text, isListening = false) }
                            } else {
                                _uiState.update { it.copy(isListening = false) }
                            }
                        }
                        is VoskVoiceManager.VoiceState.Error -> {
                            _uiState.update { it.copy(errorMessage = state.error, isListening = false) }
                        }
                        else -> {}
                    }
                }
            }

            val sample = knowledgeGraphManager.linkEvents(repository.sampleEvents())
            val sortedEvents = sortEvents(sample)
            _uiState.update {
                it.copy(
                    events = sortedEvents,
                    selectedEventId = sortedEvents.firstOrNull()?.id,
                )
            }
        }

        fun updateQuery(text: String) {
            _uiState.update { it.copy(queryText = text) }
        }

        fun updateSpeechLanguage(language: SpeechLanguage) {
            _uiState.update { it.copy(speechLanguage = language) }
        }

        fun startVoiceRecording() {
            voskVoiceManager.startListening()
        }

        fun stopVoiceRecording() {
            voskVoiceManager.stopListening()
        }

        fun setListening(listening: Boolean) {
            _uiState.update { it.copy(isListening = listening) }
        }

        fun updateTimelineZoom(value: Float) {
            _uiState.update { it.copy(timelineZoom = value) }
        }

        fun selectEvent(id: String) {
            _uiState.update { it.copy(selectedEventId = id) }
        }

        fun setZoom(zoom: Float) {
            _uiState.update { it.copy(timelineZoom = zoom) }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun setApiSettingsVisible(visible: Boolean) {
            _uiState.update { it.copy(showApiSettings = visible) }
        }

        fun setMapTileWarning(msg: String?) {
            _uiState.update { it.copy(mapTileWarning = msg) }
        }

        fun updateApiKey(value: String) {
            _uiState.update { it.copy(apiKey = value) }
        }

        fun saveApiKey() {
            viewModelScope.launch(dispatcherProvider.io) {
                globalConfigRepository.saveTimelineMapApiKey(_uiState.value.apiKey.trim())
                _uiState.update { it.copy(showApiSettings = false) }
            }
        }

        fun generateTimeline() {
            viewModelScope.launch(dispatcherProvider.main) {
                val query = _uiState.value.queryText.trim()
                if (query.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "请输入历史事件问题") }
                    return@launch
                }

                val currentApiKey = globalConfigRepository.getEffectiveTimelineMapApiKey().first().trim()

                if (!networkMonitor.isConnected.value) {
                    _uiState.update { it.copy(errorMessage = "当前处于无网络环境，无法生成大模型内容。\n将使用本地示例数据展示。") }
                }

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        errorMessage = if (!networkMonitor.isConnected.value) "当前处于无网络环境，无法生成大模型内容。\n将使用本地示例数据展示。" else null,
                    )
                }

                val modelName = globalConfigRepository.getTimelineMapModelName().first()
                val baseUrl = globalConfigRepository.getTimelineMapBaseUrl().first()

                val result =
                    if (networkMonitor.isConnected.value && currentApiKey.isNotEmpty()) {
                        repository.generateEvents(
                            query = query,
                            apiKey = currentApiKey,
                            model = modelName,
                            baseUrl = baseUrl,
                        )
                    } else if (currentApiKey.isEmpty() && networkMonitor.isConnected.value) {
                        Result.failure(Exception("API Key未配置"))
                    } else {
                        Result.failure(Exception("No network connection"))
                    }
                val events = result.getOrElse { repository.sampleEvents() }
                val linked = knowledgeGraphManager.linkEvents(events)
                val sortedEvents = sortEvents(linked)

                _uiState.update { state ->
                    state.copy(
                        events = sortedEvents,
                        selectedEventId = sortedEvents.firstOrNull()?.id,
                        errorMessage =
                            if (result.isFailure) {
                                val reason = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: "请求失败"
                                if (currentApiKey.isEmpty()) {
                                    "未配置API Key，已使用内置示例数据"
                                } else {
                                    "已使用内置示例数据（原因：$reason）"
                                }
                            } else {
                                null
                            },
                        isLoading = false,
                    )
                }
            }
        }

        private fun sortEvents(events: List<HistoricalEvent>): List<HistoricalEvent> {
            return events.sortedBy { DateUtils.parseDateKey(it.time) }
        }
    }
