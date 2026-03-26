package com.example.ai_tutor.multimodal_chat.services

import com.example.ai_tutor.timeline_map.services.KnowledgeGraphManager
import com.example.common.config.AppConstants
import com.example.common.network.llm.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AgentDecisionHub(
    private val repository: LlmRepository,
    private val knowledgeGraph: KnowledgeGraphManager,
    private val toolsIntegrator: ToolsIntegrator,
) {
    private val intentClassifier = IntentClassifier()

    suspend fun processUserRequest(
        input: MultimodalInput,
        history: List<ChatMessage>,
        apiKey: String,
        modelName: String = "qwen-turbo",
        baseUrl: String = AppConstants.BASE_URL,
    ): Flow<String> =
        flow {
            // 1. Goal Management & Intent Recognition
            val intent = intentClassifier.classify(input.text)

            when (intent) {
                is UserIntent.ToolExecution -> {
                    emit(toolsIntegrator.executeTool(intent.toolName, intent.parameters))
                }
                is UserIntent.KnowledgeQuery -> {
                    // 2. Knowledge Graph Lookup (Precision Diagnosis)
                    val knowledge = knowledgeGraph.searchKnowledge(intent.query)
                    var contextEnhancement = ""

                    if (knowledge.isNotEmpty()) {
                        val kp = knowledge.first()
                        contextEnhancement = "[Context: User is asking about ${kp.name}. Definition: ${kp.description}. Related: ${kp.relatedPoints.joinToString(
                            ", ",
                        )}]"
                        // Emit a quick indicator that we found something (optional, but good for UI feedback)
                        // emit("[Knowledge Graph] Found info on ${kp.name}...")
                    }

                    // 3. AI Chat with Context
                    // We append the context to the user's message effectively
                    val augmentedInput =
                        if (contextEnhancement.isNotEmpty()) {
                            "$contextEnhancement\n\nUser Question: ${input.text}"
                        } else {
                            input.text
                        }

                    repository.sendMessage(
                        apiKey = apiKey,
                        modelName = modelName,
                        prompt = augmentedInput,
                        history = history,
                        baseUrl = baseUrl,
                    ).collect { response ->
                        emit(response)
                    }
                }
                is UserIntent.GeneralChat, UserIntent.Unknown -> {
                    repository.sendMessage(
                        apiKey = apiKey,
                        modelName = modelName,
                        prompt = input.text,
                        history = history,
                        baseUrl = baseUrl,
                    ).collect { response ->
                        emit(response)
                    }
                }
            }
        }
}
