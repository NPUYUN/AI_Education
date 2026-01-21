package com.example.ai_tutor.core.knowledge

/**
 * Represents a node in the knowledge graph (e.g., a specific math concept).
 */
data class KnowledgeNode(
    val id: String,
    val name: String,
    val type: String, // e.g., "Concept", "Formula", "Example"
    val content: String
)

/**
 * Represents a relationship between two knowledge nodes.
 */
data class KnowledgeEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val relationType: String // e.g., "prerequisite", "is_a", "related_to"
)

/**
 * Interface for accessing and managing the Subject Knowledge Graph.
 */
class KnowledgeGraphManager {

    // In-memory graph structure for demonstration
    private val nodes = mutableMapOf<String, KnowledgeNode>()
    private val edges = mutableListOf<KnowledgeEdge>()

    init {
        // Initialize with some dummy data
        addNode(KnowledgeNode("1", "Linear Algebra", "Topic", "Branch of mathematics..."))
        addNode(KnowledgeNode("2", "Vector", "Concept", "Geometric object with magnitude and direction."))
        addNode(KnowledgeNode("3", "Matrix", "Concept", "Rectangular array of numbers."))
        addEdge("1", "2", "contains")
        addEdge("1", "3", "contains")
        addEdge("2", "3", "related_to")
    }

    fun addNode(node: KnowledgeNode) {
        nodes[node.id] = node
    }

    fun addEdge(fromId: String, toId: String, relation: String) {
        edges.add(KnowledgeEdge(fromId, toId, relation))
    }

    fun getRelatedConcepts(nodeId: String): List<KnowledgeNode> {
        val relatedIds = edges.filter { it.fromNodeId == nodeId }.map { it.toNodeId } +
                         edges.filter { it.toNodeId == nodeId }.map { it.fromNodeId }
        
        return relatedIds.distinct().mapNotNull { nodes[it] }
    }

    fun findPath(startId: String, endId: String): List<String> {
        // Simple pathfinding logic could go here
        return emptyList()
    }
    
    fun search(query: String): List<KnowledgeNode> {
        return nodes.values.filter { it.name.contains(query, ignoreCase = true) }
    }
}
