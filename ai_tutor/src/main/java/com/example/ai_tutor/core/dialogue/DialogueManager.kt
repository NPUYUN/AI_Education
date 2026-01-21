package com.example.ai_tutor.core.dialogue

/**
 * Manages the conversation flow, context, and intent recognition.
 */
class DialogueManager {

    private val conversationHistory = mutableListOf<Message>()
    private val maxHistorySize = 10

    data class Message(val role: String, val content: String)

    enum class Intent {
        GREETING,
        QUESTION,
        EXPLANATION_REQUEST,
        PRACTICE_REQUEST,
        UNKNOWN
    }

    fun processInput(input: String): DialogueContext {
        val intent = recognizeIntent(input)
        updateHistory("user", input)
        return DialogueContext(input, intent, conversationHistory.toList())
    }

    fun addResponse(response: String) {
        updateHistory("assistant", response)
    }

    private fun recognizeIntent(input: String): Intent {
        // Simple rule-based intent recognition for demo
        // Ideally, this would use a TFLite Text Classification model
        val lowerInput = input.lowercase()
        return when {
            lowerInput.contains("hello") || lowerInput.contains("hi") -> Intent.GREETING
            lowerInput.contains("what is") || lowerInput.contains("how to") -> Intent.QUESTION
            lowerInput.contains("explain") -> Intent.EXPLANATION_REQUEST
            lowerInput.contains("practice") || lowerInput.contains("quiz") -> Intent.PRACTICE_REQUEST
            else -> Intent.UNKNOWN
        }
    }

    private fun updateHistory(role: String, content: String) {
        if (conversationHistory.size >= maxHistorySize) {
            conversationHistory.removeAt(0)
        }
        conversationHistory.add(Message(role, content))
    }

    data class DialogueContext(
        val currentInput: String,
        val detectedIntent: Intent,
        val history: List<Message>
    )
}
