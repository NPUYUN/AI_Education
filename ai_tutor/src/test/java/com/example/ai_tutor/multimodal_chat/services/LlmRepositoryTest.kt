package com.example.ai_tutor.multimodal_chat.services

import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.RetrofitClient
import com.example.common.network.llm.ChatResponse
import com.example.common.network.llm.ChatChoice
import com.example.common.network.llm.ChatMessage
import com.example.common.network.llm.OpenAiService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LlmRepositoryTest {

    private lateinit var repository: LlmRepository
    private val testDispatcher = StandardTestDispatcher()
    
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
        override val unconfined = testDispatcher
    }

    private val mockService = mockk<OpenAiService>()
    private val mockRetrofit = mockk<Retrofit>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockkObject(RetrofitClient)
        every { RetrofitClient.create(any(), any()) } returns mockRetrofit
        every { mockRetrofit.create(OpenAiService::class.java) } returns mockService
        
        repository = LlmRepository(dispatcherProvider)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `sendMessage emits successful response`() = runTest(testDispatcher) {
        val expectedText = "Hello from AI"
        val mockResponse = ChatResponse(
            choices = listOf(
                ChatChoice(message = ChatMessage(role = "assistant", content = expectedText))
            ),
            output = null,
            usage = null,
            requestId = null
        )
        
        coEvery { mockService.chat(any()) } returns mockResponse

        val result = repository.sendMessage(
            apiKey = "test_key",
            prompt = "Hi",
            history = emptyList()
        ).toList()

        assertEquals(1, result.size)
        assertEquals(expectedText, result[0])
    }

    @Test
    fun `sendMessage emits error when HttpException 401 occurs`() = runTest(testDispatcher) {
        val errorResponse = Response.error<ChatResponse>(
            401,
            "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull())
        )
        coEvery { mockService.chat(any()) } throws HttpException(errorResponse)

        val result = repository.sendMessage(
            apiKey = "invalid_key",
            prompt = "Hi",
            history = emptyList()
        ).toList()

        assertEquals(1, result.size)
        assertEquals("Error: API Key 无效或未授权，请在设置中检查您的 API Key。", result[0])
    }

    @Test
    fun `sendMessage emits error when generic exception occurs`() = runTest(testDispatcher) {
        coEvery { mockService.chat(any()) } throws RuntimeException("Network timeout")

        val result = repository.sendMessage(
            apiKey = "test_key",
            prompt = "Hi",
            history = emptyList()
        ).toList()

        assertEquals(1, result.size)
        assertEquals("Error: Network timeout", result[0])
    }
}
