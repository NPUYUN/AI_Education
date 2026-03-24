package com.example.summarizer.dialogue_summarizer.presentation.viewmodels

import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ChatDao
import com.example.common.database.models.ChatSessionEntity
import com.example.common.database.models.MessageEntity
import com.example.summarizer.dialogue_summarizer.services.DialogueSummaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DialogueSummaryViewModelTest {

    private lateinit var viewModel: DialogueSummaryViewModel
    private lateinit var mockRepository: DialogueSummaryRepository
    private lateinit var mockChatDao: ChatDao
    private lateinit var mockGlobalConfigRepository: GlobalConfigRepository
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        mockChatDao = mock()
        mockGlobalConfigRepository = mock()

        whenever(mockGlobalConfigRepository.getAiTutorApiKey()).thenReturn(flowOf("test_key"))
        whenever(mockGlobalConfigRepository.getAiTutorModelName()).thenReturn(flowOf("test_model"))
        whenever(mockGlobalConfigRepository.getAiTutorBaseUrl()).thenReturn(flowOf("test_url"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads sessions`() = runTest(testDispatcher) {
        val sessions = listOf(
            ChatSessionEntity(id = "1", userId = "default_user", title = "Session 1", timestamp = 0L, lastMessage = "")
        )
        whenever(mockChatDao.getSessions("default_user")).thenReturn(flowOf(sessions))

        viewModel = DialogueSummaryViewModel(mockRepository, mockChatDao, mockGlobalConfigRepository)
        advanceUntilIdle()

        assertEquals(sessions, viewModel.uiState.value.sessions)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `selectSession updates state`() = runTest(testDispatcher) {
        whenever(mockChatDao.getSessions("default_user")).thenReturn(flowOf(emptyList()))
        viewModel = DialogueSummaryViewModel(mockRepository, mockChatDao, mockGlobalConfigRepository)
        
        val session = ChatSessionEntity(id = "1", userId = "default_user", title = "Session 1", timestamp = 0L, lastMessage = "")
        viewModel.selectSession(session)

        assertEquals(session, viewModel.uiState.value.selectedSession)
        assertEquals("", viewModel.uiState.value.summaryResult)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `summarizeSelectedSession with empty messages sets error`() = runTest(testDispatcher) {
        whenever(mockChatDao.getSessions("default_user")).thenReturn(flowOf(emptyList()))
        viewModel = DialogueSummaryViewModel(mockRepository, mockChatDao, mockGlobalConfigRepository)
        
        val session = ChatSessionEntity(id = "1", userId = "default_user", title = "Session 1", timestamp = 0L, lastMessage = "")
        viewModel.selectSession(session)
        
        whenever(mockChatDao.getMessages("1")).thenReturn(flowOf(emptyList()))

        viewModel.summarizeSelectedSession()
        advanceUntilIdle()

        assertEquals("该对话没有内容", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSummarizing)
    }

    @Test
    fun `summarizeSelectedSession success updates summaryResult`() = runTest(testDispatcher) {
        whenever(mockChatDao.getSessions("default_user")).thenReturn(flowOf(emptyList()))
        viewModel = DialogueSummaryViewModel(mockRepository, mockChatDao, mockGlobalConfigRepository)
        
        val session = ChatSessionEntity(id = "1", userId = "default_user", title = "Session 1", timestamp = 0L, lastMessage = "")
        viewModel.selectSession(session)
        
        val messages = listOf(MessageEntity(id = 1L, sessionId = "1", role = "user", content = "Hello", timestamp = 0L))
        whenever(mockChatDao.getMessages("1")).thenReturn(flowOf(messages))
        whenever(mockRepository.summarizeDialogue(any(), any(), any(), any())).thenReturn(Result.success("Summary Result"))

        viewModel.summarizeSelectedSession()
        assertTrue(viewModel.uiState.value.isSummarizing)
        
        advanceUntilIdle()

        assertEquals("Summary Result", viewModel.uiState.value.summaryResult)
        assertFalse(viewModel.uiState.value.isSummarizing)
        assertNull(viewModel.uiState.value.error)
    }
}