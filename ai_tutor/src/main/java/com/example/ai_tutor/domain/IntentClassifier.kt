package com.example.ai_tutor.domain

sealed class UserIntent {
    data class ToolExecution(val toolName: String, val parameters: String) : UserIntent()
    data class KnowledgeQuery(val query: String) : UserIntent()
    data class GeneralChat(val text: String) : UserIntent()
    object Unknown : UserIntent()
}

class IntentClassifier {
    
    fun classify(text: String): UserIntent {
        val lowerText = text.lowercase()
        
        // Tool: Calculator
        if (lowerText.contains("calculate") || 
            lowerText.contains("solve") || 
            lowerText.matches(Regex(".*\\d+\\s*[+\\-*/]\\s*\\d+.*"))) {
            return UserIntent.ToolExecution("calculator", text)
        }
        
        // Tool: Geometry Plotter
        if (lowerText.contains("draw") || 
            lowerText.contains("plot") || 
            lowerText.contains("geometry") ||
            lowerText.contains("shape")) {
            return UserIntent.ToolExecution("geometry_plotter", text)
        }
        
        // Tool: Simulator
        if (lowerText.contains("simulate") || 
            lowerText.contains("experiment") ||
            lowerText.contains("physics")) {
            return UserIntent.ToolExecution("simulator", text)
        }

        // Knowledge Graph Query
        if (lowerText.contains("explain") || 
            lowerText.contains("what is") || 
            lowerText.contains("define") ||
            lowerText.contains("concept of")) {
            val query = text.replace(Regex("(?i)(explain|what is|define|concept of)"), "").trim()
            return UserIntent.KnowledgeQuery(query)
        }

        // Default to General Chat
        return UserIntent.GeneralChat(text)
    }
}
