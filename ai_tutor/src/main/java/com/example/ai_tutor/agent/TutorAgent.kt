package com.example.ai_tutor.agent

import com.example.ai_tutor.core.dialogue.DialogueManager
import com.example.ai_tutor.core.knowledge.KnowledgeGraphManager
import com.example.ai_tutor.data.ChatCompletionRequest
import com.example.ai_tutor.data.DeepSeekApi
import com.example.ai_tutor.data.Message
import com.example.common.network.NetworkClient

/**
 * Enhanced TutorAgent integrating Dialogue Management, Knowledge Graph, and LLM.
 */
class TutorAgent {
    private val apiKey = "Bearer sk-99858a456b8b4ddc81d926f8d6397451"
    private val baseUrl = "https://api.deepseek.com/"

    private val api: DeepSeekApi by lazy {
        NetworkClient.createService(DeepSeekApi::class.java, baseUrl)
    }

    private val dialogueManager = DialogueManager()
    private val knowledgeGraphManager = KnowledgeGraphManager()

    suspend fun processQuery(query: String): String {
        return try {
            // 1. Process Input via Dialogue Manager (Intent Recognition, History)
            val context = dialogueManager.processInput(query)
            
            // 2. Query Knowledge Graph (Optional, based on intent/content)
            val relatedConcepts = knowledgeGraphManager.search(context.currentInput)
            val knowledgeContext = if (relatedConcepts.isNotEmpty()) {
                "\nRelated Concepts: ${relatedConcepts.joinToString { it.name }}"
            } else ""

            // 3. Construct Prompt for LLM
            val systemPrompt = "You are a helpful AI Tutor. Detected Intent: ${context.detectedIntent}.$knowledgeContext"
            
            val messages = listOf(
                Message(role = "system", content = systemPrompt)
            ) + context.history.map { Message(it.role, it.content) }

            val request = ChatCompletionRequest(messages = messages)
            val response = api.chatCompletion(apiKey, request)
            
            val aiContent = response.choices.firstOrNull()?.message?.content ?: "Error: No response from AI."
            
            // 4. Update Dialogue History with Response
            dialogueManager.addResponse(aiContent)
            
            aiContent
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
