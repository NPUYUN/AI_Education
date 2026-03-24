package com.example.review.planner.presentation.viewmodels

import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.models.ErrorBookEntity
import com.example.review.planner.services.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

import com.example.common.database.PreferencesManager

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private lateinit var viewModel: ReviewViewModel
    private lateinit var mockRepository: ReviewRepository
    private lateinit var mockErrorBookDao: ErrorBookDao
    private lateinit var mockGlobalConfigRepository: GlobalConfigRepository
    private lateinit var mockPreferencesManager: PreferencesManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        mockErrorBookDao = mock()
        mockGlobalConfigRepository = mock()
        mockPreferencesManager = mock()

        whenever(mockGlobalConfigRepository.getAiTutorApiKey()).thenReturn(flowOf("test_key"))
        whenever(mockGlobalConfigRepository.getAiTutorModelName()).thenReturn(flowOf("test_model"))
        whenever(mockGlobalConfigRepository.getAiTutorBaseUrl()).thenReturn(flowOf("test_url"))
        whenever(mockErrorBookDao.getAllErrorRecords()).thenReturn(flowOf(emptyList()))
        whenever(mockPreferencesManager.getString(any(), any())).thenReturn(flowOf(""))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads error records`() = runTest(testDispatcher) {
        val records = listOf(
            ErrorBookEntity(id = 1, subject = "Math", questionContent = "Q", errorReason = "R", correctSolution = "S")
        )
        whenever(mockErrorBookDao.getAllErrorRecords()).thenReturn(flowOf(records))

        viewModel = ReviewViewModel(mockRepository, mockErrorBookDao, mockGlobalConfigRepository, mockPreferencesManager)
        advanceUntilIdle()

        assertEquals(records, viewModel.uiState.value.errorRecords)
    }

    @Test
    fun `setTab updates selectedTab`() = runTest(testDispatcher) {
        viewModel = ReviewViewModel(mockRepository, mockErrorBookDao, mockGlobalConfigRepository, mockPreferencesManager)
        advanceUntilIdle()

        viewModel.setTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `generateReviewPlan success updates plan`() = runTest(testDispatcher) {
        viewModel = ReviewViewModel(mockRepository, mockErrorBookDao, mockGlobalConfigRepository, mockPreferencesManager)
        advanceUntilIdle()

        whenever(mockRepository.generateReviewPlan(any(), any(), any(), any())).thenReturn(Result.success("My Plan"))

        viewModel.updateSubjectInput("Math")
        viewModel.generateReviewPlan()
        assertTrue(viewModel.uiState.value.isGeneratingPlan)

        advanceUntilIdle()

        assertEquals("My Plan", viewModel.uiState.value.reviewPlan)
        assertFalse(viewModel.uiState.value.isGeneratingPlan)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `generateReinforcementQuiz with empty input sets error`() = runTest(testDispatcher) {
        viewModel = ReviewViewModel(mockRepository, mockErrorBookDao, mockGlobalConfigRepository, mockPreferencesManager)
        advanceUntilIdle()

        viewModel.updateKnowledgePointInput("   ")
        viewModel.generateReinforcementQuiz()

        assertEquals("请输入要巩固的知识点", viewModel.uiState.value.error)
    }

    @Test
    fun `generateReinforcementQuiz success updates quiz`() = runTest(testDispatcher) {
        viewModel = ReviewViewModel(mockRepository, mockErrorBookDao, mockGlobalConfigRepository, mockPreferencesManager)
        advanceUntilIdle()

        viewModel.updateKnowledgePointInput("Math")
        whenever(mockRepository.generateReinforcementQuiz(any(), any(), any(), any())).thenReturn(Result.success("Quiz Data"))

        viewModel.generateReinforcementQuiz()
        assertTrue(viewModel.uiState.value.isGeneratingQuiz)

        advanceUntilIdle()

        assertEquals("Quiz Data", viewModel.uiState.value.reinforcementQuiz)
        assertFalse(viewModel.uiState.value.isGeneratingQuiz)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `deleteErrorRecord calls dao`() = runTest(testDispatcher) {
        viewModel = ReviewViewModel(mockRepository, mockErrorBookDao, mockGlobalConfigRepository, mockPreferencesManager)
        advanceUntilIdle()

        val record = ErrorBookEntity(id = 1, subject = "Math", questionContent = "Q", errorReason = "R", correctSolution = "S")
        viewModel.deleteErrorRecord(record)
        
        advanceUntilIdle()

        verify(mockErrorBookDao).deleteErrorRecord(record)
    }
}
