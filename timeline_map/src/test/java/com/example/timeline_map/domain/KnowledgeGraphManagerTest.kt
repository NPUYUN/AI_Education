package com.example.timeline_map.domain

import com.example.timeline_map.data.model.HistoricalEvent
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class KnowledgeGraphManagerTest {

    private val manager = KnowledgeGraphManager()

    private fun createEvent(
        time: String,
        location: String,
        description: String,
        people: List<String> = emptyList()
    ): HistoricalEvent {
        return HistoricalEvent(
            id = UUID.randomUUID().toString(),
            time = time,
            location = location,
            description = description,
            people = people,
            latitude = 0.0,
            longitude = 0.0
        )
    }

    @Test
    fun testLinkEvents_SameYear() {
        val e1 = createEvent("1911年", "武汉", "Event 1")
        val e2 = createEvent("1911.10.10", "北京", "Event 2")
        
        val events = listOf(e1, e2)
        val linked = manager.linkEvents(events)
        
        assertTrue(linked[0].relatedIds.contains(linked[1].id))
        assertTrue(linked[1].relatedIds.contains(linked[0].id))
    }

    @Test
    fun testLinkEvents_ShareLocation() {
        val e1 = createEvent("1900", "武汉", "Event 1")
        val e2 = createEvent("1901", "湖北武汉", "Event 2")
        
        val events = listOf(e1, e2)
        val linked = manager.linkEvents(events)
        
        assertTrue(linked[0].relatedIds.contains(linked[1].id))
    }

    @Test
    fun testLinkEvents_ShareKeywords() {
        // "革命" should be a keyword
        val e1 = createEvent("1900", "Loc A", "辛亥革命爆发")
        val e2 = createEvent("1905", "Loc B", "这是另一场革命")
        
        val events = listOf(e1, e2)
        val linked = manager.linkEvents(events)
        
        assertTrue(linked[0].relatedIds.contains(linked[1].id))
    }

    @Test
    fun testLinkEvents_StopWords() {
        // "的" is a stop word. "的人" should be filtered out or ignored?
        // extractKeywords logic: "的人" contains "的", so it is filtered.
        // If "的人" is the only common bigram, they should NOT link.
        val e1 = createEvent("1900", "Loc A", "伟大的人民") // "伟大", "大的", "的人", "人民" -> "伟大", "人民" ("大的" has "的", "的人" has "的")
        val e2 = createEvent("1905", "Loc B", "聪明的孩子") // "聪明", "明的", "的孩", "孩子" -> "聪明", "孩子"
        
        // Common bigrams without filtering: "的" related? No, bigrams are "的人", "的孩". No common bigrams.
        // What if "我是人" and "你是人"?
        // "我是" (has 是), "是人" (has 是). Both filtered.
        // "你是" (has 是), "是人" (has 是). Both filtered.
        // So no keywords extracted. No link.
        
        val e3 = createEvent("1900", "Loc A", "我是人")
        val e4 = createEvent("1905", "Loc B", "你是人")
        
        val events = listOf(e3, e4)
        val linked = manager.linkEvents(events)
        
        assertFalse(linked[0].relatedIds.contains(linked[1].id))
    }
}
