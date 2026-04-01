package com.example.review.planner.presentation.viewmodels

import com.example.common.config.GlobalConfigRepository
import com.example.common.database.PreferencesManager
import com.example.common.database.dao.ChatDao
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.models.ErrorBookEntity
import com.example.common.utils.NetworkMonitor
import com.example.review.planner.services.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    private lateinit var viewModel: ReviewViewModel
    private lateinit var mockRepository: ReviewRepository
    private lateinit var mockErrorBookDao: ErrorBookDao
    private lateinit var mockChatDao: ChatDao
    private lateinit var mockGlobalConfigRepository: GlobalConfigRepository
    private lateinit var mockPreferencesManager: PreferencesManager
    private lateinit var mockNetworkMonitor: NetworkMonitor

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        mockErrorBookDao = mock()
        mockChatDao = mock()
        mockGlobalConfigRepository = mock()
        mockPreferencesManager = mock()
        mockNetworkMonitor = mock()

        whenever(mockGlobalConfigRepository.getAiTutorApiKey()).thenReturn(flowOf("test_key"))
        whenever(mockGlobalConfigRepository.getAiTutorModelName()).thenReturn(flowOf("test_model"))
        whenever(mockGlobalConfigRepository.getAiTutorBaseUrl()).thenReturn(flowOf("test_url"))
        whenever(mockErrorBookDao.getAllErrorRecords()).thenReturn(flowOf(emptyList()))
        whenever(mockChatDao.getSessions(any())).thenReturn(flowOf(emptyList()))
        whenever(mockPreferencesManager.getString(any(), any())).thenReturn(flowOf(""))
        whenever(mockNetworkMonitor.isConnected).thenReturn(MutableStateFlow(true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads error records`() =
        runTest(testDispatcher) {
            val records =
                listOf(
                    ErrorBookEntity(id = 1, subject = "Math", questionContent = "Q", errorReason = "R", correctSolution = "S"),
                )
            whenever(mockErrorBookDao.getAllErrorRecords()).thenReturn(flowOf(records))

            viewModel =
                ReviewViewModel(
                    mockRepository,
                    mockErrorBookDao,
                    mockChatDao,
                    mockGlobalConfigRepository,
                    mockPreferencesManager,
                    mockNetworkMonitor,
                )
            advanceUntilIdle()

            assertEquals(records, viewModel.uiState.value.errorRecords)
        }

    @Test
    fun `setTab updates selectedTab`() =
        runTest(testDispatcher) {
            viewModel =
                ReviewViewModel(
                    mockRepository,
                    mockErrorBookDao,
                    mockChatDao,
                    mockGlobalConfigRepository,
                    mockPreferencesManager,
                    mockNetworkMonitor,
                )
            advanceUntilIdle()

            viewModel.setTab(2)
            assertEquals(2, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun `generateReviewPlan success updates plan`() =
        runTest(testDispatcher) {
            viewModel =
                ReviewViewModel(
                    mockRepository,
                    mockErrorBookDao,
                    mockChatDao,
                    mockGlobalConfigRepository,
                    mockPreferencesManager,
                    mockNetworkMonitor,
                )
            advanceUntilIdle()

            whenever(mockRepository.generateReviewPlan(any(), any(), any(), any(), any())).thenReturn(Result.success("My Plan"))

            viewModel.updateSubjectInput("Math")
            viewModel.generateReviewPlan()
            assertTrue(viewModel.uiState.value.isGeneratingPlan)

            advanceUntilIdle()

            assertEquals("My Plan", viewModel.uiState.value.reviewPlan)
            assertFalse(viewModel.uiState.value.isGeneratingPlan)
        }

    @Test
    fun `generateReinforcementQuiz with empty input sends error event`() =
        runTest(testDispatcher) {
            viewModel =
                ReviewViewModel(
                    mockRepository,
                    mockErrorBookDao,
                    mockChatDao,
                    mockGlobalConfigRepository,
                    mockPreferencesManager,
                    mockNetworkMonitor,
                )
            advanceUntilIdle()

            val errors = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errors)
                }

            viewModel.updateKnowledgePointInput("   ")
            viewModel.toggleUseRecentContextForQuiz(false)
            viewModel.generateReinforcementQuiz()
            advanceUntilIdle()

            assertTrue(errors.contains("请输入要巩固的知识点，或选择基于最近记录生成"))
            job.cancel()
        }

    @Test
    fun `generateReinforcementQuiz success updates quiz`() =
        runTest(testDispatcher) {
            viewModel =
                ReviewViewModel(
                    mockRepository,
                    mockErrorBookDao,
                    mockChatDao,
                    mockGlobalConfigRepository,
                    mockPreferencesManager,
                    mockNetworkMonitor,
                )
            advanceUntilIdle()

            viewModel.updateKnowledgePointInput("Math")
            whenever(mockRepository.generateReinforcementQuiz(any(), any(), any(), any(), any())).thenReturn(Result.success("Quiz Data"))

            viewModel.generateReinforcementQuiz()
            assertTrue(viewModel.uiState.value.isGeneratingQuiz)

            advanceUntilIdle()

            assertEquals("Quiz Data", viewModel.uiState.value.reinforcementQuiz)
            assertFalse(viewModel.uiState.value.isGeneratingQuiz)
        }

    @Test
    fun `deleteErrorRecord calls dao`() =
        runTest(testDispatcher) {
            viewModel =
                ReviewViewModel(
                    mockRepository,
                    mockErrorBookDao,
                    mockChatDao,
                    mockGlobalConfigRepository,
                    mockPreferencesManager,
                    mockNetworkMonitor,
                )
            advanceUntilIdle()

            val record = ErrorBookEntity(id = 1, subject = "Math", questionContent = "Q", errorReason = "R", correctSolution = "S")
            viewModel.deleteErrorRecord(record)

            advanceUntilIdle()

            verify(mockErrorBookDao).deleteErrorRecord(record)
        }
}
