package com.example.timeline_map.domain

import com.example.timeline_map.data.model.HistoricalEvent

class KnowledgeGraphManager {
    fun linkEvents(events: List<HistoricalEvent>): List<HistoricalEvent> {
        return events.map { event ->
            val related = events.filter { other ->
                other.id != event.id && (
                    sharePeople(event, other) ||
                        shareLocation(event, other) ||
                        sameYear(event, other)
                    )
            }.map { it.id }
            event.copy(relatedIds = related)
        }
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
        val match = Regex("(\\d{4})").find(time) ?: return null
        return match.groupValues[1].toIntOrNull()
    }
}
