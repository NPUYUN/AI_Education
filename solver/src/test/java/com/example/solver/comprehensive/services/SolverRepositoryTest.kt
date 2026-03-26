package com.example.solver.comprehensive.services

import com.example.common.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SolverRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var repository: SolverRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dispatcherProvider =
            object : DispatcherProvider {
                override val main: CoroutineDispatcher = testDispatcher
                override val io: CoroutineDispatcher = testDispatcher
                override val default: CoroutineDispatcher = testDispatcher
                override val unconfined: CoroutineDispatcher = testDispatcher
            }
        repository = SolverRepository(dispatcherProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `solveProblem returns failure when apiKey is empty`() =
        runTest {
            val result =
                repository.solveProblem(
                    apiKey = "",
                    baseUrl = "http://localhost",
                    modelName = "test-model",
                    systemPrompt = "prompt",
                    questionText = "question",
                    base64Image = null,
                )

            assertTrue(result.isFailure)
            assertEquals("API Key 不能为空", result.exceptionOrNull()?.message)
        }

    @Test
    fun `solveProblem returns failure when baseUrl is empty`() =
        runTest {
            val result =
                repository.solveProblem(
                    apiKey = "key",
                    baseUrl = "",
                    modelName = "test-model",
                    systemPrompt = "prompt",
                    questionText = "question",
                    base64Image = null,
                )

            assertTrue(result.isFailure)
            assertEquals("Base URL 不能为空", result.exceptionOrNull()?.message)
        }

    @Test
    fun `solveProblem returns failure when questionText is empty and no image provided`() =
        runTest {
            val result =
                repository.solveProblem(
                    apiKey = "key",
                    baseUrl = "http://localhost",
                    modelName = "test-model",
                    systemPrompt = "prompt",
                    questionText = "",
                    base64Image = null,
                )

            assertTrue(result.isFailure)
            assertEquals("问题内容不能为空", result.exceptionOrNull()?.message)
        }
}
