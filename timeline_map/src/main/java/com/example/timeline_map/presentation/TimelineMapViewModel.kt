package com.example.timeline_map.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeline_map.data.model.HistoricalEvent
import com.example.timeline_map.data.model.SpeechLanguage
import com.example.timeline_map.data.repository.TimelineRepository
import com.example.timeline_map.domain.KnowledgeGraphManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TimelineMapViewModel : ViewModel() {
    private val repository = TimelineRepository()
    private val knowledgeGraphManager = KnowledgeGraphManager()

    private val _queryText = mutableStateOf("")
    val queryText: State<String> = _queryText

    private val _apiKey = mutableStateOf("")
    val apiKey: State<String> = _apiKey

    private val _speechLanguage = mutableStateOf(SpeechLanguage.AUTO)
    val speechLanguage: State<SpeechLanguage> = _speechLanguage

    private val _isListening = mutableStateOf(false)
    val isListening: State<Boolean> = _isListening

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _events = mutableStateListOf<HistoricalEvent>()
    val events: List<HistoricalEvent> get() = _events

    private val _selectedEventId = mutableStateOf<String?>(null)
    val selectedEventId: State<String?> = _selectedEventId

    private val _timelineZoom = mutableStateOf(1f)
    val timelineZoom: State<Float> = _timelineZoom

    init {
        val sample = knowledgeGraphManager.linkEvents(repository.sampleEvents())
        _events.addAll(sortEvents(sample))
        _selectedEventId.value = _events.firstOrNull()?.id
    }

    fun updateQuery(text: String) {
        _queryText.value = text
    }

    fun updateApiKey(text: String) {
        _apiKey.value = text
    }

    fun updateSpeechLanguage(language: SpeechLanguage) {
        _speechLanguage.value = language
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

    fun generateTimeline() {
        val query = _queryText.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "请输入历史事件问题"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val key = _apiKey.value.trim()
            val result = if (key.isNotEmpty()) {
                repository.generateEvents(query, key)
            } else {
                Result.failure(IllegalStateException("缺少API Key"))
            }
            val events = result.getOrElse {
                if (query.contains("辛亥革命")) {
                    repository.sampleEvents()
                } else {
                    repository.sampleEvents()
                }
            }
            val linked = knowledgeGraphManager.linkEvents(events)
            _events.clear()
            _events.addAll(sortEvents(linked))
            _selectedEventId.value = _events.firstOrNull()?.id
            if (result.isFailure) {
                _errorMessage.value = "已使用内置示例数据"
            }
            _isLoading.value = false
        }
    }

    private fun sortEvents(events: List<HistoricalEvent>): List<HistoricalEvent> {
        return events.sortedBy { parseDateKey(it.time) }
    }

    private fun parseDateKey(time: String): Long {
        val yearMatch = Regex("(\\d{4})").find(time)?.groupValues?.get(1)
        val year = yearMatch?.toIntOrNull() ?: return Long.MAX_VALUE
        val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM", "yyyy/MM", "yyyy")
        for (format in formats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(format)
                val date = when (format) {
                    "yyyy" -> LocalDate.of(year, 1, 1)
                    "yyyy-MM", "yyyy/MM" -> LocalDate.parse("$time-01", DateTimeFormatter.ofPattern("$format-dd"))
                    else -> LocalDate.parse(time, formatter)
                }
                return date.toEpochDay()
            } catch (_: DateTimeParseException) {
            }
        }
        return Long.MAX_VALUE
    }
}
