package com.example.timeline_map.presentation

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.manager.VoskVoiceManager
import com.example.timeline_map.data.model.HistoricalEvent
import com.example.timeline_map.data.model.SpeechLanguage
import com.example.timeline_map.data.repository.TimelineRepository
import com.example.timeline_map.domain.KnowledgeGraphManager
import com.example.common.database.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TimelineMapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimelineRepository()
    private val knowledgeGraphManager = KnowledgeGraphManager()
    
    private val preferences = PreferencesManager(application)
    private val apiKeyKey = "bailian_api_key"
    private var apiKey = ""
    
    private val voskVoiceManager = VoskVoiceManager(application)

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

    init {
        voskVoiceManager.init(viewModelScope)
        
        viewModelScope.launch {
            preferences.getString(apiKeyKey).collectLatest { key ->
                apiKey = key
            }
        }
        
        viewModelScope.launch {
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

    fun generateTimeline() {
        if (apiKey.isBlank()) {
            _errorMessage.value = "请先在设置或视频总结模块中填写 API Key"
            return
        }
        val query = _queryText.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "请输入历史事件问题"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.generateEvents(query, apiKey)
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
        return events.sortedBy { parseDateKey(it.time) }
    }

    private fun parseDateKey(time: String): Long {
        // Handle "BC" or "公元前"
        val isBC = time.contains("前") || time.contains("BC", ignoreCase = true)
        
        // Extract year, month, day using regex
        val yearMatch = Regex("(\\d{1,4})\\s*(年|-|/)").find(time) ?: Regex("(\\d{1,4})").find(time)
        val monthMatch = Regex("(\\d{1,2})\\s*(月|-|/)").find(time)
        val dayMatch = Regex("(\\d{1,2})\\s*(日|号)").find(time)

        val yearStr = yearMatch?.groupValues?.get(1)
        if (yearStr != null) {
            var year = yearStr.toIntOrNull() ?: return Long.MAX_VALUE
            if (isBC) {
                year = -year
            }
            val month = monthMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
            val day = dayMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 28) ?: 1
            
            // To properly sort BC years, we can just use a simple calculated value or epoch day
            // Since LocalDate doesn't support 0 or negative years easily with simple of(), we calculate an approximate sort key
            return year.toLong() * 10000 + month * 100 + day
        }
        return Long.MAX_VALUE
    }

}
