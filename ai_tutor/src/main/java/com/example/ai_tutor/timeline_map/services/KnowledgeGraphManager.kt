package com.example.ai_tutor.timeline_map.services

import com.example.ai_tutor.timeline_map.models.HistoricalEvent
import com.example.ai_tutor.timeline_map.models.KnowledgePoint

interface KnowledgeGraphManager {
    suspend fun getKnowledgePoint(id: String): KnowledgePoint?
    suspend fun findRelatedPoints(id: String): List<KnowledgePoint>
    suspend fun searchKnowledge(query: String): List<KnowledgePoint>
    fun linkEvents(events: List<HistoricalEvent>): List<HistoricalEvent>
}

// Mock implementation for now
class MockKnowledgeGraphManager : KnowledgeGraphManager {
    private val mockDb = mapOf(
        "math_001" to KnowledgePoint("math_001", "Pythagorean Theorem", "Math", "a^2 + b^2 = c^2", listOf("math_002")),
        "math_002" to KnowledgePoint("math_002", "Right Triangle", "Math", "Triangle with 90 degree angle", listOf("math_001"))
    )

    override suspend fun getKnowledgePoint(id: String): KnowledgePoint? = mockDb[id]

    override suspend fun findRelatedPoints(id: String): List<KnowledgePoint> {
        val point = mockDb[id] ?: return emptyList()
        return point.relatedPoints.mapNotNull { mockDb[it] }
    }
    
    override suspend fun searchKnowledge(query: String): List<KnowledgePoint> {
        return mockDb.values.filter { it.name.contains(query, ignoreCase = true) }
    }
    
    override fun linkEvents(events: List<HistoricalEvent>): List<HistoricalEvent> {
        return events.mapIndexed { index, event ->
            val linked = mutableListOf<String>()
            if (index > 0) linked.add(events[index - 1].id)
            if (index < events.size - 1) linked.add(events[index + 1].id)
            event.copy(linkedEventIds = linked)
        }
    }
}
