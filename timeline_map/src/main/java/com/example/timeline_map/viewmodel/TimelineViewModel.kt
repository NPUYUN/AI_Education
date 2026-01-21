package com.example.timeline_map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeline_map.data.HistoricalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimelineViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<HistoricalEvent>>(emptyList())
    val events: StateFlow<List<HistoricalEvent>> = _events

    private val _speechResult = MutableStateFlow("")
    val speechResult: StateFlow<String> = _speechResult

    fun onSpeechResult(text: String) {
        _speechResult.value = text
        processQuery(text)
    }

    private fun processQuery(query: String) {
        // Mock logic: if query contains "Revolution", show 1911 events
        if (query.contains("辛亥革命")) {
            _events.value = listOf(
                HistoricalEvent("1", "武昌起义", 1911, "辛亥革命开端", 30.54, 114.30),
                HistoricalEvent("2", "中华民国成立", 1912, "孙中山就任临时大总统", 32.06, 118.79)
            )
        }
    }
}
