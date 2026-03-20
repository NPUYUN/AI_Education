package com.example.timeline_map.presentation

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.manager.VoskVoiceManager
import com.example.timeline_map.data.model.HistoricalEvent
import com.example.timeline_map.data.model.SpeechLanguage
import com.example.timeline_map.data.repository.TimelineRepository
import com.example.timeline_map.domain.KnowledgeGraphManager
import com.example.common.database.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

import com.example.common.config.AppConstants
import com.example.common.dispatchers.DispatcherProvider

import com.example.timeline_map.domain.util.DateUtils

@HiltViewModel
class TimelineMapViewModel @Inject constructor(
    private val preferences: PreferencesManager,
    private val voskVoiceManager: VoskVoiceManager,
    private val repository: TimelineRepository,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {
    private val knowledgeGraphManager = KnowledgeGraphManager()
    
    private val apiKeyKey = "api_key_timeline_map"

    private val _queryText = mutableStateOf("")
    val queryText: State<String> = _queryText

    private val _speechLanguage = mutableStateOf(SpeechLanguage.AUTO)
    val speechLanguage: State<SpeechLanguage> = _speechLanguage

    private val _isListening = mutableStateOf(false)
    val isListening: State<Boolean> = _isListening

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    private val _mapTileWarning = mutableStateOf<String?>(null)
    val mapTileWarning: State<String?> = _mapTileWarning

    private val _events = mutableStateListOf<HistoricalEvent>()
    val events: List<HistoricalEvent> get() = _events

    private val _selectedEventId = mutableStateOf<String?>(null)
    val selectedEventId: State<String?> = _selectedEventId

    private val _timelineZoom = mutableStateOf(1f)
    val timelineZoom: State<Float> = _timelineZoom
    
    // API Key State
    private val _apiKey = mutableStateOf("")
    val apiKey: State<String> = _apiKey
    
    private val _modelName = mutableStateOf(AppConstants.DEFAULT_MODEL_NAME)
    private val _baseUrl = mutableStateOf(AppConstants.BASE_URL)

    private val _showApiSettings = mutableStateOf(false)
    val showApiSettings: State<Boolean> = _showApiSettings

    init {
        voskVoiceManager.init(viewModelScope)
        
        viewModelScope.launch(dispatcherProvider.main) {
            preferences.getString(apiKeyKey).collectLatest { key ->
                _apiKey.value = key.ifBlank {
                    // Fallback to old bailian key
                    preferences.getString("bailian_api_key", "").first()
                }
            }
        }

        viewModelScope.launch(dispatcherProvider.io) {
            preferences.getString("model_name_timeline_map", AppConstants.DEFAULT_MODEL_NAME).collectLatest {
                _modelName.value = it
            }
        }

        viewModelScope.launch(dispatcherProvider.io) {
            preferences.getString("base_url_timeline_map", AppConstants.BASE_URL).collectLatest {
                _baseUrl.value = it
            }
        }
        
        viewModelScope.launch(dispatcherProvider.main) {
            voskVoiceManager.voiceState.collectLatest { state ->
                when (state) {
                    is VoskVoiceManager.VoiceState.Listening -> _isListening.value = true
                    is VoskVoiceManager.VoiceState.Result -> {
                         if (state.text.isNotEmpty()) {
                             _queryText.value = state.text
                         }
                         _isListening.value = false
                    }
                    is VoskVoiceManager.VoiceState.Error -> {
                        _errorMessage.value = state.error
                        _isListening.value = false
                    }
                    else -> {}
                }
            }
        }

        val sample = knowledgeGraphManager.linkEvents(repository.sampleEvents())
        _events.addAll(sortEvents(sample))
        _selectedEventId.value = _events.firstOrNull()?.id
    }

    fun updateQuery(text: String) {
        _queryText.value = text
    }

    fun updateSpeechLanguage(language: SpeechLanguage) {
        _speechLanguage.value = language
    }

    fun startVoiceRecording() {
        voskVoiceManager.startListening()
    }

    fun stopVoiceRecording() {
        voskVoiceManager.stopListening()
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun updateTimelineZoom(value: Float) {
        _timelineZoom.value = value
    }

    fun selectEvent(eventId: String) {
        _selectedEventId.value = eventId
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    fun setMapTileWarning(msg: String?) {
        _mapTileWarning.value = msg
    }

    fun updateApiKey(value: String) {
        _apiKey.value = value
    }

    fun saveApiKey() {
        viewModelScope.launch(dispatcherProvider.io) {
            preferences.saveString(apiKeyKey, _apiKey.value.trim())
        }
    }

    fun generateTimeline() {
        val query = _queryText.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "请输入历史事件问题"
            return
        }
        viewModelScope.launch(dispatcherProvider.main) {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.generateEvents(
                query = query,
                apiKey = _apiKey.value.trim(),
                model = _modelName.value,
                baseUrl = _baseUrl.value
            )
            val events = result.getOrElse { repository.sampleEvents() }
            val linked = knowledgeGraphManager.linkEvents(events)
            _events.clear()
            _events.addAll(sortEvents(linked))
            _selectedEventId.value = _events.firstOrNull()?.id
            if (result.isFailure) {
                val reason = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: "请求失败"
                _errorMessage.value = "已使用内置示例数据（原因：$reason）"
            }
            _isLoading.value = false
        }
    }

    private fun sortEvents(events: List<HistoricalEvent>): List<HistoricalEvent> {
        return events.sortedBy { DateUtils.parseDateKey(it.time) }
    }


}
