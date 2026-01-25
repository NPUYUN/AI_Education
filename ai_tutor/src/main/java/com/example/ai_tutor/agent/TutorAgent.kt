package com.example.ai_tutor.agent

import android.graphics.Bitmap
import android.util.Base64
import com.example.ai_tutor.core.dialogue.DialogueManager
import com.example.ai_tutor.core.knowledge.KnowledgeGraphManager
import com.example.ai_tutor.data.ChatCompletionRequest
import com.example.ai_tutor.data.ContentPart
import com.example.ai_tutor.data.QwenApi
import com.example.ai_tutor.data.ImageUrl
import com.example.ai_tutor.data.Message
import com.example.ai_tutor.data.StreamOptions
import com.example.common.network.NetworkClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader

/**
 * Enhanced TutorAgent integrating Dialogue Management, Knowledge Graph, and LLM.
 */
class TutorAgent {
    private val apiKey = "Bearer sk-e6a46e1940de419caf8e5b010954a7e3"
    private val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/"

    private val api: QwenApi by lazy {
        NetworkClient.createService(QwenApi::class.java, baseUrl)
    }

    private val dialogueManager = DialogueManager()
    private val knowledgeGraphManager = KnowledgeGraphManager()
    private val gson = Gson()

    suspend fun processQueryStream(query: String, image: Bitmap? = null): Flow<String> = flow {
        try {
            // 1. Process Input via Dialogue Manager (Intent Recognition, History)
            val context = dialogueManager.processInput(query)
            
            // 2. Query Knowledge Graph (Optional, based on intent/content)
            val relatedConcepts = knowledgeGraphManager.search(context.currentInput)
            val knowledgeContext = if (relatedConcepts.isNotEmpty()) {
                "\nRelated Concepts: ${relatedConcepts.joinToString { it.name }}"
            } else ""

            // 3. Construct Prompt for LLM
            val systemPrompt = "You are a helpful AI Tutor. Detected Intent: ${context.detectedIntent}.$knowledgeContext"
            
            val messages = mutableListOf<Message>()
            messages.add(Message(role = "system", content = systemPrompt))
            
            // Add history
            messages.addAll(context.history.map { Message(it.role, it.content) })
            
            // Add current message (Multimodal or Text)
            if (image != null) {
                val base64Image = withContext(Dispatchers.Default) {
                    encodeBitmap(image)
                }
                val contentParts = listOf(
                    ContentPart(type = "text", text = query),
                    ContentPart(type = "image_url", image_url = ImageUrl("data:image/jpeg;base64,$base64Image"))
                )
                
                if (messages.isNotEmpty() && messages.last().role == "user") {
                    messages.removeAt(messages.lastIndex)
                }
                messages.add(Message(role = "user", content = contentParts))
            } 

            val request = ChatCompletionRequest(
                messages = messages, 
                stream = true,
                stream_options = StreamOptions(include_usage = true)
            )
            
            val responseBody = api.chatCompletionStream(apiKey, request)
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            
            val fullContentBuilder = StringBuilder()
            
            var line = reader.readLine()
            while (line != null) {
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()
                    if (data == "[DONE]") break
                    
                    try {
                        val jsonObject = gson.fromJson(data, JsonObject::class.java)
                        val choices = jsonObject.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val choice = choices.get(0).asJsonObject
                            val delta = choice.getAsJsonObject("delta")
                            if (delta != null) {
                                if (delta.has("content")) {
                                    val content = delta.get("content").asString
                                    if (content.isNotEmpty()) {
                                        emit(content)
                                        fullContentBuilder.append(content)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for partial chunks
                    }
                }
                line = reader.readLine()
            }
            
            // 4. Update Dialogue History with Full Response
            dialogueManager.addResponse(fullContentBuilder.toString())
            
        } catch (e: Exception) {
            e.printStackTrace()
            emit("Error: ${e.message}")
        }
    }

    suspend fun processQuery(query: String, image: Bitmap? = null): String {
        return try {
            // 1. Process Input via Dialogue Manager (Intent Recognition, History)
            val context = dialogueManager.processInput(query)
            
            // 2. Query Knowledge Graph (Optional, based on intent/content)
            val relatedConcepts = knowledgeGraphManager.search(context.currentInput)
            val knowledgeContext = if (relatedConcepts.isNotEmpty()) {
                "\nRelated Concepts: ${relatedConcepts.joinToString { it.name }}"
            } else ""

            // 3. Construct Prompt for LLM
            val systemPrompt = "You are a helpful AI Tutor. Detected Intent: ${context.detectedIntent}.$knowledgeContext\nPlease show your deep thinking process before your final answer."
            
            val messages = mutableListOf<Message>()
            messages.add(Message(role = "system", content = systemPrompt))
            
            // Add history
            messages.addAll(context.history.map { Message(it.role, it.content) })
            
            // Determine model
            var model = "qwen-plus"
            
            // Add current message (Multimodal or Text)
            if (image != null) {
                model = "qwen-vl-max"
                // If image is present, construct a multimodal message
                // Note: If the backend model does not support vision, this might fail.
                // We attempt to send it. If it fails, we should fallback or the user needs a vision-capable model.
                val base64Image = encodeBitmap(image)
                val contentParts = listOf(
                    ContentPart(type = "text", text = query),
                    ContentPart(type = "image_url", image_url = ImageUrl("data:image/jpeg;base64,$base64Image"))
                )
                // Remove the last text-only message added by DialogueManager (which was just 'query')
                // Actually, DialogueManager.processInput adds to history. 
                // We should use the 'messages' list we are building for the API call.
                // context.history already contains the *current* input as the last item?
                // Let's check DialogueManager.
                // DialogueManager.processInput adds the user message to history.
                // So context.history has the user message as the last item.
                // But that message has text content only.
                // We should REPLACE the last message in our 'messages' list with the multimodal one.
                
                if (messages.isNotEmpty() && messages.last().role == "user") {
                    messages.removeAt(messages.lastIndex)
                }
                messages.add(Message(role = "user", content = contentParts))
            } 
            // If image is null, the message is already in context.history as text (added by dialogueManager)

            val request = ChatCompletionRequest(model = model, messages = messages)
            val response = api.chatCompletion(apiKey, request)
            
            val aiContent = response.choices.firstOrNull()?.message?.content ?: "Error: No response from AI."
            
            // 4. Update Dialogue History with Response
            dialogueManager.addResponse(aiContent)
            
            aiContent
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: If multimodal fails, try text-only (using the query which likely contains OCR text)
            if (image != null) {
                 return processQuery(query, null)
            }
            "Error: ${e.message}"
        }
    }

    private fun encodeBitmap(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too large? For now, compress to JPEG 80%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
