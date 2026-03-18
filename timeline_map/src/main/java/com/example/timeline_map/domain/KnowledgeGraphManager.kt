package com.example.timeline_map.domain

import com.example.timeline_map.data.model.HistoricalEvent

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
        // Extract words from title (length >= 2)
        val wordsA = extractKeywords(a.title)
        val wordsB = extractKeywords(b.title)
        return wordsA.intersect(wordsB).isNotEmpty()
    }

    private fun extractKeywords(text: String): Set<String> {
        // Very basic keyword extraction for Chinese text, splitting by punctuation
        // and finding substrings of length >= 2
        val cleaned = text.replace(Regex("[\\p{Punct}\\s]+"), "")
        val keywords = mutableSetOf<String>()
        if (cleaned.length >= 2) {
            for (i in 0..cleaned.length - 2) {
                keywords.add(cleaned.substring(i, i + 2))
            }
        }
        return keywords
    }

    private fun sharePeople(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        return a.people.any { person -> b.people.contains(person) }
    }

    private fun shareLocation(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        return a.location.isNotBlank() && a.location == b.location
    }

    private fun sameYear(a: HistoricalEvent, b: HistoricalEvent): Boolean {
        val yearA = extractYear(a.time)
        val yearB = extractYear(b.time)
        return yearA != null && yearA == yearB
    }

    private fun extractYear(time: String): Int? {
        val isBC = time.contains("前") || time.contains("BC", ignoreCase = true)
        val match = Regex("(\\d{1,4})\\s*(年|-|/)").find(time) ?: Regex("(\\d{1,4})").find(time)
        val yearStr = match?.groupValues?.get(1) ?: return null
        var year = yearStr.toIntOrNull() ?: return null
        if (isBC) {
            year = -year
        }
        return year
    }
}
