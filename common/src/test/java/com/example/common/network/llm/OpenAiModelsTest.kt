package com.example.common.network.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenAiModelsTest {

    @Test
    fun `ChatRequest initializes correctly with basic properties`() {
        val message = ChatMessage(role = "user", content = "Hello")
        val request = ChatRequest(
            model = "test-model",
            messages = listOf(message)
        )

        assertEquals("test-model", request.model)
        assertEquals(1, request.messages.size)
        assertEquals("user", request.messages[0].role)
        assertEquals("Hello", request.messages[0].content)
    }

    @Test
    fun `ChatRequest initializes correctly with parameters`() {
        val parameters = ChatParameters(resultFormat = "text")
        val request = ChatRequest(
            model = "test-model",
            messages = listOf(),
            parameters = parameters
        )

        assertNotNull(request.parameters)
        assertEquals("text", request.parameters?.resultFormat)
    }

    @Test
    fun `ContentItem initializes correctly for text`() {
        val contentItem = ContentItem(type = "text", text = "This is text")
        
        assertEquals("text", contentItem.type)
        assertEquals("This is text", contentItem.text)
    }

    @Test
    fun `ContentItem initializes correctly for image_url`() {
        val imageUrl = ImageUrl(url = "http://example.com/image.png")
        val contentItem = ContentItem(type = "image_url", imageUrl = imageUrl)
        
        assertEquals("image_url", contentItem.type)
        assertNotNull(contentItem.imageUrl)
        assertEquals("http://example.com/image.png", contentItem.imageUrl?.url)
    }

    @Test
    fun `ChatResponse initializes correctly`() {
        val message = ChatMessage(role = "assistant", content = "Response text")
        val choice = ChatChoice(message = message, finishReason = "stop")
        val output = ChatOutput(text = "Output text", finishReason = "stop", choices = listOf(choice))
        val usage = ChatUsage(totalTokens = 10, inputTokens = 5, outputTokens = 5)
        
        val response = ChatResponse(
            output = output,
            choices = listOf(choice),
            usage = usage,
            requestId = "req-123"
        )
        
        assertNotNull(response.output)
        assertEquals("Output text", response.output?.text)
        
        assertNotNull(response.choices)
        assertEquals(1, response.choices?.size)
        assertEquals("stop", response.choices?.get(0)?.finishReason)
        
        assertNotNull(response.usage)
        assertEquals(10, response.usage?.totalTokens)
        
        assertEquals("req-123", response.requestId)
    }
}
