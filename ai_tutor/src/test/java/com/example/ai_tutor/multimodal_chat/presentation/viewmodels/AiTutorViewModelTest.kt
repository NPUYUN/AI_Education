package com.example.ai_tutor.multimodal_chat.presentation.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.ai_tutor.multimodal_chat.services.LlmRepository
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ChatDao
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.manager.VoskVoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AiTutorViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AiTutorViewModel

    private val application: Application = mock()
    private val globalConfigRepository: GlobalConfigRepository = mock()
    private val chatDao: ChatDao = mock()
    private val voskVoiceManager: VoskVoiceManager = mock()
    private val llmRepository: LlmRepository = mock()

    private val testDispatcher = StandardTestDispatcher()

    private val dispatcherProvider =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
            override val unconfined = testDispatcher
        }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(chatDao.getSessions(anyOrNull())).thenReturn(flowOf(emptyList()))
        whenever(globalConfigRepository.getEffectiveAiTutorApiKey()).thenReturn(flowOf("test_key"))
        whenever(globalConfigRepository.getAiTutorBaseUrl()).thenReturn(flowOf("test_url"))
        whenever(globalConfigRepository.getAiTutorModelName()).thenReturn(flowOf("test_model"))
        whenever(voskVoiceManager.voiceState).thenReturn(MutableStateFlow(VoskVoiceManager.VoiceState.Ready))
        kotlinx.coroutines.runBlocking {
            whenever(
                llmRepository.sendMessage(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()),
            ).thenReturn(flowOf("Mocked response"))
        }

        viewModel =
            AiTutorViewModel(
                application = application,
                globalConfigRepository = globalConfigRepository,
                chatDao = chatDao,
                voskVoiceManager = voskVoiceManager,
                dispatcherProvider = dispatcherProvider,
                repository = llmRepository,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() =
        runTest(testDispatcher) {
            val state = viewModel.uiState.value
            assertEquals("", state.inputText)
            assertEquals(false, state.isLoading)
            assertEquals(false, state.showApiSettings)
            assertNull(state.errorMessage)
        }

    @Test
    fun `onInputChanged updates text`() =
        runTest(testDispatcher) {
            viewModel.onInputChanged("Hello AI")
            assertEquals("Hello AI", viewModel.uiState.value.inputText)
        }

    @Test
    fun `clearErrorMessage removes error`() =
        runTest(testDispatcher) {
            viewModel.onInputChanged("trigger error setup")
            viewModel.clearErrorMessage()
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `setApiSettingsVisible updates state correctly`() =
        runTest(testDispatcher) {
            viewModel.setApiSettingsVisible(true)
            assertEquals(true, viewModel.uiState.value.showApiSettings)

            viewModel.setApiSettingsVisible(false)
            assertEquals(false, viewModel.uiState.value.showApiSettings)
        }

    @Test
    fun `onSuggestionClicked updates input text and sends message`() =
        runTest(testDispatcher) {
            val suggestion = "如何制定高效的学习计划?"
            viewModel.onSuggestionClicked(suggestion)
            // Since sendMessage is called, inputText will be cleared, and a message is added
            assertEquals("", viewModel.uiState.value.inputText)
            assertEquals(true, viewModel.uiState.value.isLoading)
            // Check if user message is added
            val lastMessage = viewModel.uiState.value.messages.lastOrNull()
            assertEquals("user", lastMessage?.role)
            assertEquals(suggestion, lastMessage?.content)
        }

    @Test
    fun `voice recording controls interact with vosk manager`() =
        runTest(testDispatcher) {
            viewModel.startVoiceRecording()
            org.mockito.kotlin.verify(voskVoiceManager).startListening()

            viewModel.stopVoiceRecording()
            org.mockito.kotlin.verify(voskVoiceManager).stopListening()

            viewModel.cancelVoiceRecording()
            // verify it's called again
            org.mockito.kotlin.verify(voskVoiceManager, org.mockito.kotlin.times(2)).stopListening()
        }

    @Test
    fun `startNewChat clears input and messages`() =
        runTest(testDispatcher) {
            viewModel.onInputChanged("Some text")
            viewModel.startNewChat()
            advanceUntilIdle()
            assertEquals("", viewModel.uiState.value.inputText)
            assertEquals(emptyList(), viewModel.uiState.value.messages)
            org.mockito.kotlin.verify(chatDao, org.mockito.kotlin.times(2)).insertSession(anyOrNull()) // 1 from init, 1 from startNewChat
        }
}
