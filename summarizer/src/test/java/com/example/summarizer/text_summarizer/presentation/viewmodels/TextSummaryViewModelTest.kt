package com.example.summarizer.text_summarizer.presentation.viewmodels

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.common.config.GlobalConfigRepository
import com.example.summarizer.text_summarizer.services.TextExtractionService
import com.example.summarizer.text_summarizer.services.TextSummaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class TextSummaryViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: TextSummaryRepository

    @Mock
    private lateinit var textExtractionService: TextExtractionService

    @Mock
    private lateinit var globalConfigRepository: GlobalConfigRepository

    private lateinit var viewModel: TextSummaryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        `when`(globalConfigRepository.getEffectiveVideoSummaryApiKey()).thenReturn(flowOf("test_key"))
        `when`(globalConfigRepository.getVideoSummaryBaseUrl()).thenReturn(flowOf("test_url"))
        `when`(globalConfigRepository.getVideoSummaryModelName()).thenReturn(flowOf("test_model"))

        viewModel = TextSummaryViewModel(repository, textExtractionService, globalConfigRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateInputText updates uiState correctly`() {
        viewModel.updateInputText("Hello World")
        assertEquals("Hello World", viewModel.uiState.value.inputText)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `summarize with empty input sets error`() {
        viewModel.updateInputText("   ")
        viewModel.summarize()
        assertEquals("输入文本不能为空", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSummarizing)
    }

    @Test
    fun `summarize successful updates summaryResult`() =
        runTest(testDispatcher) {
            val inputText = "Some text"
            val summaryResult = "Summary text"
            viewModel.updateInputText(inputText)

            `when`(repository.summarizeText("test_key", inputText, "test_model", "test_url"))
                .thenReturn(Result.success(summaryResult))

            viewModel.summarize()

            assertTrue(viewModel.uiState.value.isSummarizing)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSummarizing)
            assertEquals(summaryResult, viewModel.uiState.value.summaryResult)
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun `summarize failure sets error`() =
        runTest(testDispatcher) {
            val inputText = "Some text"
            val errorMsg = "Network Error"
            viewModel.updateInputText(inputText)

            `when`(repository.summarizeText("test_key", inputText, "test_model", "test_url"))
                .thenReturn(Result.failure(Exception(errorMsg)))

            viewModel.summarize()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSummarizing)
            assertEquals(errorMsg, viewModel.uiState.value.error)
        }

    @Test
    fun `handleFileUri success updates inputText`() =
        runTest(testDispatcher) {
            val uri = mock<Uri>()
            val extractedText = "Extracted text from file"

            `when`(textExtractionService.extractTextFromUri(uri))
                .thenReturn(Result.success(extractedText))

            viewModel.handleFileUri(uri)

            assertTrue(viewModel.uiState.value.isExtractingFile)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isExtractingFile)
            assertEquals(extractedText, viewModel.uiState.value.inputText)
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun `handleFileUri failure sets error`() =
        runTest(testDispatcher) {
            val uri = mock<Uri>()
            val errorMsg = "File read error"

            `when`(textExtractionService.extractTextFromUri(uri))
                .thenReturn(Result.failure(Exception(errorMsg)))

            viewModel.handleFileUri(uri)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isExtractingFile)
            assertEquals(errorMsg, viewModel.uiState.value.error)
        }

    @Test
    fun `clearError sets error to null`() {
        viewModel.updateInputText("  ")
        viewModel.summarize()
        assertEquals("输入文本不能为空", viewModel.uiState.value.error)

        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.error)
    }
}
