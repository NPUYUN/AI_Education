package com.example.solver.comprehensive.presentation.viewmodels

import android.content.Context
import android.net.Uri
import com.example.common.config.GlobalConfigRepository
import com.example.solver.comprehensive.services.SolverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SolverViewModelTest {

    private lateinit var viewModel: SolverViewModel
    private lateinit var mockRepository: SolverRepository
    private lateinit var mockGlobalConfigRepository: GlobalConfigRepository
    private lateinit var mockErrorBookDao: com.example.common.database.dao.ErrorBookDao
    private lateinit var mockContext: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        mockGlobalConfigRepository = mock()
        mockErrorBookDao = mock()
        mockContext = mock()

        whenever(mockGlobalConfigRepository.getAiTutorApiKey()).thenReturn(flowOf("test_key"))
        whenever(mockGlobalConfigRepository.getAiTutorModelName()).thenReturn(flowOf("test_model"))
        whenever(mockGlobalConfigRepository.getAiTutorBaseUrl()).thenReturn(flowOf("test_url"))

        viewModel = SolverViewModel(mockRepository, mockGlobalConfigRepository, mockErrorBookDao, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals(0, state.selectedTab)
        assertEquals("", state.questionText)
        assertNull(state.imageUri)
        assertFalse(state.isSolving)
        assertEquals("", state.solutionResult)
        assertNull(state.error)
    }

    @Test
    fun `setTab updates selectedTab and clears results`() {
        viewModel.updateQuestionText("Question")
        viewModel.setTab(1)
        val state = viewModel.uiState.value
        assertEquals(1, state.selectedTab)
        assertEquals("", state.questionText) // As per current implementation, it clears
        assertEquals("", state.solutionResult)
    }

    @Test
    fun `updateQuestionText updates text and clears error`() {
        viewModel.updateQuestionText("New Question")
        val state = viewModel.uiState.value
        assertEquals("New Question", state.questionText)
        assertNull(state.error)
    }

    @Test
    fun `setImageUri updates uri and clears error`() {
        val uri = mock<Uri>()
        viewModel.setImageUri(uri)
        val state = viewModel.uiState.value
        assertEquals(uri, state.imageUri)
        assertNull(state.error)
    }

    @Test
    fun `solveProblem with empty input sets error`() = runTest(testDispatcher) {
        viewModel.solveProblem()
        assertEquals("请输入题目或上传题目图片", viewModel.uiState.value.error)
    }

    @Test
    fun `solveProblem success updates solutionResult`() = runTest(testDispatcher) {
        viewModel.updateQuestionText("1+1=?")
        
        whenever(mockRepository.solveProblem(any(), any(), any(), any(), any(), isNull())).thenReturn(Result.success("2"))

        viewModel.solveProblem()
        assertTrue(viewModel.uiState.value.isSolving)
        
        advanceUntilIdle()

        assertEquals("2", viewModel.uiState.value.solutionResult)
        assertFalse(viewModel.uiState.value.isSolving)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `solveProblem failure sets error`() = runTest(testDispatcher) {
        viewModel.updateQuestionText("1+1=?")
        
        whenever(mockRepository.solveProblem(any(), any(), any(), any(), any(), isNull())).thenReturn(Result.failure(Exception("API Error")))

        viewModel.solveProblem()
        
        advanceUntilIdle()

        assertEquals("API Error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSolving)
    }

    @Test
    fun `clearError clears error state`() {
        viewModel.updateQuestionText("1+1=?") // Just to have something
        // forcefully set error by calling solve with empty
        viewModel.updateQuestionText("")
        viewModel.solveProblem()
        assertNotNull(viewModel.uiState.value.error)
        
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
