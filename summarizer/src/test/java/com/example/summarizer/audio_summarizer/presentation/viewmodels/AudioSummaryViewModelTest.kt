package com.example.summarizer.audio_summarizer.presentation.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.common.config.GlobalConfigRepository
import com.example.common.dispatchers.DispatcherProvider
import com.example.summarizer.audio_summarizer.services.AudioSummaryRepository
import com.example.summarizer.video_summarizer.services.SherpaAsrManager
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AudioSummaryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
        override val unconfined = testDispatcher
    }

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var cursor: Cursor

    @Mock
    private lateinit var repository: AudioSummaryRepository

    @Mock
    private lateinit var sherpaAsrManager: SherpaAsrManager

    @Mock
    private lateinit var globalConfigRepository: GlobalConfigRepository

    private lateinit var viewModel: AudioSummaryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        `when`(context.contentResolver).thenReturn(contentResolver)
        
        `when`(globalConfigRepository.getEffectiveVideoSummaryApiKey()).thenReturn(flowOf("test_key"))
        `when`(globalConfigRepository.getVideoSummaryBaseUrl()).thenReturn(flowOf("test_url"))
        `when`(globalConfigRepository.getVideoSummaryModelName()).thenReturn(flowOf("test_model"))
        
        viewModel = AudioSummaryViewModel(
            context, 
            repository, 
            sherpaAsrManager, 
            globalConfigRepository,
            dispatcherProvider
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleAudioUri resolves file name and updates state`() {
        val uri = mock<Uri>()
        
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn("test_audio.mp3")
        
        viewModel.handleAudioUri(uri)
        
        assertEquals(uri, viewModel.uiState.value.selectedAudioUri)
        assertEquals("test_audio.mp3", viewModel.uiState.value.selectedAudioName)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `processAudio with no uri sets error`() {
        viewModel.processAudio()
        assertEquals("请先选择一个音频文件", viewModel.uiState.value.error)
    }

    @Test
    fun `processAudio successful flow updates state`() = runTest(testDispatcher) {
        val uri = mock<Uri>()
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(null)
        viewModel.handleAudioUri(uri)
        
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        `when`(context.cacheDir).thenReturn(tempDir)
        
        val dummyAudioContent = "dummy audio data".toByteArray()
        val inputStream = ByteArrayInputStream(dummyAudioContent)
        `when`(contentResolver.openInputStream(uri)).thenReturn(inputStream)
        
        val transcript = "Extracted transcript"
        `when`(sherpaAsrManager.transcribe(any())).thenReturn(transcript)
        
        val summaryResult = "Final summary"
        `when`(repository.summarizeAudioTranscript("test_key", transcript, "test_model", "test_url"))
            .thenReturn(Result.success(summaryResult))

        viewModel.processAudio()
        
        assertTrue(viewModel.uiState.value.isTranscribing)
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isTranscribing)
        assertFalse(viewModel.uiState.value.isSummarizing)
        assertEquals(transcript, viewModel.uiState.value.transcriptResult)
        assertEquals(summaryResult, viewModel.uiState.value.summaryResult)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `processAudio empty transcript sets error`() = runTest(testDispatcher) {
        val uri = mock<Uri>()
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(null)
        viewModel.handleAudioUri(uri)
        
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        `when`(context.cacheDir).thenReturn(tempDir)
        
        val dummyAudioContent = "dummy audio data".toByteArray()
        val inputStream = ByteArrayInputStream(dummyAudioContent)
        `when`(contentResolver.openInputStream(uri)).thenReturn(inputStream)
        
        `when`(sherpaAsrManager.transcribe(any())).thenReturn("   ")

        viewModel.processAudio()
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isTranscribing)
        assertFalse(viewModel.uiState.value.isSummarizing)
        assertEquals("音频转写结果为空", viewModel.uiState.value.error)
    }
    
    @Test
    fun `clearError sets error to null`() {
        viewModel.processAudio()
        assertEquals("请先选择一个音频文件", viewModel.uiState.value.error)
        
        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.error)
    }
}
