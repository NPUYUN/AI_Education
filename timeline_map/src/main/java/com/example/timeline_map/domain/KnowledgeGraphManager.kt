package com.example.timeline_map.domain

import com.example.timeline_map.data.model.HistoricalEvent
import com.example.timeline_map.domain.util.DateUtils

class KnowledgeGraphManager {
    fun linkEvents(events: List<HistoricalEvent>): List<HistoricalEvent> {
        return events.map { event ->
            val related = events.filter { other ->
                other.id != event.id && (
                    sharePeople(event, other) ||
                        shareLocation(event, other) ||
                        sameYear(event, other) ||
                        shareKeywords(event, other)
                    )
            }.map { it.id }
            event.copy(relatedIds = related)
        }
    }

    private fun shareKeywords(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        // Extract words from description (length >= 2)
        val wordsA = extractKeywords(a.description)
        val wordsB = extractKeywords(b.description)
        return wordsA.intersect(wordsB).isNotEmpty()
    }

    private val stopWords = setOf(
        "的", "了", "着", "地", "得", "是", "在", "有", "和", "与", "及", "或", 
        "这", "那", "哪", "个", "只", "把", "被", "让", "给", "对", "于", 
        "之", "为", "以", "可", "其", "但", "并", "已", "将", "乃", "若", "则", 
        "所", "由", "向", "从", "到", "自", "等", "它", "他", "她", "你", "我"
    )

    private fun extractKeywords(text: String): Set<String> {
        // Very basic keyword extraction for Chinese text, splitting by punctuation
        // and finding substrings of length >= 2
        val cleaned = text.replace(Regex("[\\p{Punct}\\s]+"), "")
        val keywords = mutableSetOf<String>()
        if (cleaned.length >= 2) {
            for (i in 0..cleaned.length - 2) {
                val word = cleaned.substring(i, i + 2)
                // Filter out if the bigram contains any stop word
                // This is aggressive but reduces noise significantly
                if (word.none { it.toString() in stopWords }) {
                    keywords.add(word)
                }
            }
        }
        return keywords
    }

    private fun sharePeople(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        return a.people.any { person -> b.people.contains(person) }
    }

    private fun shareLocation(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        if (a.location.isBlank() || b.location.isBlank()) return false
        // Allow containment if length is sufficient to avoid over-matching (e.g. "中国")
        // But for cities like "武汉" and "湖北武汉", containment is good.
        // Let's require at least 2 chars for containment check
        if (a.location.length < 2 || b.location.length < 2) {
            return a.location == b.location
        }
        return a.location == b.location || a.location.contains(b.location) || b.location.contains(a.location)
    }

    private fun sameYear(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        val yearA = DateUtils.extractYear(a.time)
        val yearB = DateUtils.extractYear(b.time)
        return yearA != null && yearA == yearB
    }

}
