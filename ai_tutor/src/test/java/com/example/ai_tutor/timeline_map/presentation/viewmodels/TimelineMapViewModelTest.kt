package com.example.ai_tutor.timeline_map.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.ai_tutor.timeline_map.models.HistoricalEvent
import com.example.ai_tutor.timeline_map.services.TimelineRepository
import com.example.common.config.GlobalConfigRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class TimelineMapViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var globalConfigRepository: GlobalConfigRepository

    @Mock
    private lateinit var voskVoiceManager: VoskVoiceManager

    @Mock
    private lateinit var repository: TimelineRepository

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var viewModel: TimelineMapViewModel

    private val mockVoiceState = MutableStateFlow<VoskVoiceManager.VoiceState>(VoskVoiceManager.VoiceState.Ready)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        dispatcherProvider = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
            override val unconfined = testDispatcher
        }

        `when`(globalConfigRepository.getEffectiveTimelineMapApiKey()).thenReturn(flowOf("test_api_key"))
        `when`(globalConfigRepository.getTimelineMapModelName()).thenReturn(flowOf("test_model"))
        `when`(globalConfigRepository.getTimelineMapBaseUrl()).thenReturn(flowOf("test_base_url"))
        
        `when`(voskVoiceManager.voiceState).thenReturn(mockVoiceState)
        
        `when`(repository.sampleEvents()).thenReturn(listOf(
            HistoricalEvent(id = "1", time = "2000-01-01", location = "Location 1", description = "Desc 1", people = listOf("Person 1"), latitude = 0.0, longitude = 0.0),
            HistoricalEvent(id = "2", time = "2001-01-01", location = "Location 2", description = "Desc 2", people = listOf("Person 2"), latitude = 1.0, longitude = 1.0)
        ))

        viewModel = TimelineMapViewModel(
            globalConfigRepository = globalConfigRepository,
            voskVoiceManager = voskVoiceManager,
            repository = repository,
            dispatcherProvider = dispatcherProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads configs correctly`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("test_api_key", state.apiKey)
        assertEquals("test_model", state.modelName)
        assertEquals("test_base_url", state.baseUrl)
        assertEquals(2, state.events.size)
        assertEquals("1", state.selectedEventId)
    }

    @Test
    fun `updateQuery updates uiState`() {
        viewModel.updateQuery("new query")
        assertEquals("new query", viewModel.uiState.value.queryText)
    }

    @Test
    fun `voice recording controls interact with vosk manager`() = runTest(testDispatcher) {
        viewModel.startVoiceRecording()
        verify(voskVoiceManager).startListening()

        viewModel.stopVoiceRecording()
        verify(voskVoiceManager).stopListening()
    }

    @Test
    fun `voice state changes update uiState`() = runTest(testDispatcher) {
        mockVoiceState.value = VoskVoiceManager.VoiceState.Listening
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isListening)

        mockVoiceState.value = VoskVoiceManager.VoiceState.Result("recognized text")
        advanceUntilIdle()
        assertEquals("recognized text", viewModel.uiState.value.queryText)
        assertFalse(viewModel.uiState.value.isListening)

        mockVoiceState.value = VoskVoiceManager.VoiceState.Error("voice error")
        advanceUntilIdle()
        assertEquals("voice error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun `generateTimeline with empty query sets error`() {
        viewModel.updateQuery("   ")
        viewModel.generateTimeline()
        assertEquals("请输入历史事件问题", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `generateTimeline success updates events`() = runTest(testDispatcher) {
        viewModel.updateQuery("history")
        
        val newEvents = listOf(
            HistoricalEvent(id = "3", time = "2010-01-01", location = "Location 3", description = "New Desc", people = listOf("Person 3"), latitude = 2.0, longitude = 2.0)
        )
        `when`(repository.generateEvents(any(), any(), any(), any())).thenReturn(Result.success(newEvents))
        
        viewModel.generateTimeline()
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.events.size)
        assertEquals("3", viewModel.uiState.value.selectedEventId)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `generateTimeline failure falls back to sample events`() = runTest(testDispatcher) {
        viewModel.updateQuery("history")
        
        `when`(repository.generateEvents(any(), any(), any(), any())).thenReturn(Result.failure(Exception("API Error")))
        
        viewModel.generateTimeline()
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.events.size)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("已使用内置示例数据") == true)
    }
}
