package com.example.solver.comprehensive.presentation.viewmodels

import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.dao.SolveHistoryDao
import com.example.solver.comprehensive.services.SolverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class SolverViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: SolverRepository
    private lateinit var globalConfigRepository: GlobalConfigRepository
    private lateinit var errorBookDao: ErrorBookDao
    private lateinit var solveHistoryDao: SolveHistoryDao

    private lateinit var viewModel: SolverViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mock()
        globalConfigRepository = mock()
        errorBookDao = mock()
        solveHistoryDao = mock()
        val networkMonitor = mock<com.example.common.utils.NetworkMonitor>()
        val isConnectedFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
        whenever(networkMonitor.isConnected).thenReturn(isConnectedFlow)

        whenever(globalConfigRepository.getAiTutorApiKey()).thenReturn(flowOf("test-key"))
        whenever(globalConfigRepository.getAiTutorModelName()).thenReturn(flowOf("test-model"))
        whenever(globalConfigRepository.getAiTutorBaseUrl()).thenReturn(flowOf("http://test-url"))

        whenever(solveHistoryDao.getRecent(any())).thenReturn(flowOf(emptyList()))
        whenever(solveHistoryDao.getAll()).thenReturn(flowOf(emptyList()))

        viewModel =
            SolverViewModel(
                repository,
                globalConfigRepository,
                errorBookDao,
                solveHistoryDao,
                networkMonitor,
                mock(),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setTab updates selected tab and clears states`() =
        runTest(testDispatcher) {
            viewModel.updateQuestionText("old question")
            viewModel.setTab(1)

            val state = viewModel.uiState.value
            assertEquals(1, state.selectedTab)
            assertEquals("", state.questionText)
            assertEquals("", state.solutionResult)
            assertFalse(state.isAddedToErrorBook)
        }

    @Test
    fun `updateQuestionText updates text and auto-classifies tab`() =
        runTest(testDispatcher) {
            // Geometry
            viewModel.updateQuestionText("求三角形面积")
            assertEquals("求三角形面积", viewModel.uiState.value.questionText)
            assertEquals(0, viewModel.uiState.value.selectedTab)

            // Algebra
            viewModel.updateQuestionText("解二次方程")
            assertEquals(1, viewModel.uiState.value.selectedTab)

            // Comprehensive (Physics)
            viewModel.updateQuestionText("求加速度大小")
            assertEquals(2, viewModel.uiState.value.selectedTab)
            assertEquals("物理", viewModel.uiState.value.comprehensiveType)
        }

    @Test
    fun `solveProblem returns error when questionText is empty and imageUri is null`() =
        runTest(testDispatcher) {
            val errors = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errors)
                }
            viewModel.updateQuestionText("")
            viewModel.setImageUri(null)

            viewModel.solveProblem()
            advanceUntilIdle()

            assertTrue(errors.contains("请输入题目或上传题目图片"))
            job.cancel()
        }

    @Test
    fun `solveProblem handles network timeout error`() =
        runTest(testDispatcher) {
            val errors = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errors)
                }
            viewModel.updateQuestionText("三角形面积")
            whenever(repository.solveProblem(any(), any(), any(), any(), any(), org.mockito.kotlin.anyOrNull()))
                .thenReturn(Result.failure(SocketTimeoutException("Timeout")))

            viewModel.solveProblem()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSolving)
            assertTrue(errors.contains("网络请求超时，请稍后重试"))
            job.cancel()
        }

    @Test
    fun `solveProblem handles unknown host error`() =
        runTest(testDispatcher) {
            val errors = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errors)
                }
            viewModel.updateQuestionText("三角形面积")
            whenever(repository.solveProblem(any(), any(), any(), any(), any(), org.mockito.kotlin.anyOrNull()))
                .thenReturn(Result.failure(UnknownHostException("Unknown host")))

            viewModel.solveProblem()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSolving)
            assertTrue(errors.contains("无法连接到服务器，请检查网络设置"))
            job.cancel()
        }

    @Test
    fun `solveProblem handles success`() =
        runTest(testDispatcher) {
            viewModel.updateQuestionText("求三角形面积")
            whenever(repository.solveProblem(any(), any(), any(), any(), any(), org.mockito.kotlin.anyOrNull()))
                .thenReturn(Result.success("面积为10"))

            viewModel.solveProblem()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSolving)
            assertEquals("面积为10", viewModel.uiState.value.solutionResult)
            verify(solveHistoryDao).insert(any())
        }

    @Test
    fun `addToErrorBook saves correctly`() =
        runTest(testDispatcher) {
            viewModel.updateQuestionText("测试题目")
            whenever(repository.solveProblem(any(), any(), any(), any(), any(), org.mockito.kotlin.anyOrNull()))
                .thenReturn(Result.success("解答内容"))

            viewModel.solveProblem()
            advanceUntilIdle()

            viewModel.addToErrorBook("数学", "粗心")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isAddedToErrorBook)
            verify(errorBookDao).insertErrorRecord(any())
        }

    @Test
    fun `updateQuestionText identifies function problems correctly`() =
        runTest(testDispatcher) {
            viewModel.updateQuestionText("求函数y=x^2的极值")
            assertTrue(viewModel.uiState.value.isFunction)

            viewModel.updateQuestionText("求三角形面积")
            assertFalse(viewModel.uiState.value.isFunction)
        }

    @Test
    fun `solveProblem parses geometry drawing steps correctly`() =
        runTest(testDispatcher) {
            viewModel.updateQuestionText("画一个圆")

            val jsonResponse =
                """
                这是解答。
                BEGIN_DRAWING_JSON
                [
                  {
                    "title": "画一个圆",
                    "type": "2D",
                    "shapes": [
                      {
                        "kind": "circle",
                        "cx": 0,
                        "cy": 0,
                        "r": 5,
                        "color": "blue"
                      }
                    ]
                  }
                ]
                END_DRAWING_JSON
                """.trimIndent()

            whenever(repository.solveProblem(any(), any(), any(), any(), any(), org.mockito.kotlin.anyOrNull()))
                .thenReturn(Result.success(jsonResponse))

            viewModel.solveProblem()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSolving)
            assertTrue(viewModel.uiState.value.solutionResult.contains("这是解答。"))
            assertFalse(viewModel.uiState.value.solutionResult.contains("BEGIN_DRAWING_JSON"))
            assertEquals(1, viewModel.uiState.value.drawingSteps.size)
            assertEquals("画一个圆", viewModel.uiState.value.drawingSteps[0].title)
            assertEquals(1, viewModel.uiState.value.drawingSteps[0].shapes.size)
            assertEquals("circle", viewModel.uiState.value.drawingSteps[0].shapes[0]["kind"])
        }

    @Test
    fun `solveProblem parses invalid geometry drawing steps gracefully`() =
        runTest(testDispatcher) {
            viewModel.updateQuestionText("画一个圆")

            val invalidJsonResponse =
                """
                这是解答。
                BEGIN_DRAWING_JSON
                [
                  {
                    "title": "画一个圆",
                    "type": "2D",
                    "shapes": [
                      {
                        "kind": "circle",
                        "cx": 0,
                        "cy": 0,
                        "r": 5,
                        "color": "blue"
                      }
                    ]
                  }
                END_DRAWING_JSON
                """.trimIndent()
            // Missing closing bracket

            whenever(repository.solveProblem(any(), any(), any(), any(), any(), org.mockito.kotlin.anyOrNull()))
                .thenReturn(Result.success(invalidJsonResponse))

            viewModel.solveProblem()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSolving)
            assertTrue(viewModel.uiState.value.solutionResult.contains("这是解答。"))
            assertFalse(viewModel.uiState.value.solutionResult.contains("BEGIN_DRAWING_JSON"))
            assertEquals(0, viewModel.uiState.value.drawingSteps.size) // Should be empty due to parsing exception
        }
}
