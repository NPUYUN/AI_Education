package com.example.summarizer.videosummarizer.presentation.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.common.config.GlobalConfigRepository
import com.example.common.dispatchers.DispatcherProvider
import com.example.summarizer.videosummarizer.services.ModelDownloader
import com.example.summarizer.videosummarizer.services.ProcessLocalVideoUseCase
import com.example.summarizer.videosummarizer.services.SherpaAsrManager
import com.example.summarizer.videosummarizer.services.SummarizeVideoUseCase
import com.example.summarizer.videosummarizer.services.VideoDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class VideoDownloadViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val dispatcherProvider =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
            override val unconfined = testDispatcher
        }

    @Mock private lateinit var application: Application

    @Mock private lateinit var downloader: VideoDownloader

    @Mock private lateinit var sherpaAsrManager: SherpaAsrManager

    @Mock private lateinit var globalConfigRepository: GlobalConfigRepository

    @Mock private lateinit var modelDownloader: ModelDownloader

    @Mock private lateinit var processLocalVideoUseCase: ProcessLocalVideoUseCase

    @Mock private lateinit var summarizeVideoUseCase: SummarizeVideoUseCase

    @Mock private lateinit var networkMonitor: com.example.common.utils.NetworkMonitor

    @Mock private lateinit var summaryHistoryDao: com.example.common.database.dao.SummaryHistoryDao

    private lateinit var viewModel: VideoDownloadViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        `when`(globalConfigRepository.getCurrentUserId()).thenReturn(flowOf("user123"))
        `when`(globalConfigRepository.getEffectiveVideoSummaryApiKey()).thenReturn(flowOf("test_api_key"))
        `when`(globalConfigRepository.getVideoSummaryModelName()).thenReturn(flowOf("test_model"))
        `when`(globalConfigRepository.getVideoSummaryBaseUrl()).thenReturn(flowOf("test_base_url"))

        `when`(modelDownloader.isModelReady()).thenReturn(true) // Skip model download in simple tests

        `when`(networkMonitor.isConnected).thenReturn(MutableStateFlow(true))

        viewModel =
            VideoDownloadViewModel(
                application,
                downloader,
                sherpaAsrManager,
                globalConfigRepository,
                modelDownloader,
                dispatcherProvider,
                processLocalVideoUseCase,
                summarizeVideoUseCase,
                networkMonitor,
                summaryHistoryDao,
            )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads configs correctly`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertEquals("user123", state.currentUserId)
            assertEquals("test_api_key", state.apiKey)
            assertEquals("test_model", state.modelName)
            assertEquals("test_base_url", state.baseUrl)
        }

    @Test
    fun `updateInputUrl updates state`() {
        viewModel.updateInputUrl("http://example.com/video")
        assertEquals("http://example.com/video", viewModel.uiState.value.inputUrl)
    }

    @Test
    fun `setApiSettingsVisible updates state`() {
        viewModel.setApiSettingsVisible(true)
        assertTrue(viewModel.uiState.value.showApiSettings)

        viewModel.setApiSettingsVisible(false)
        assertFalse(viewModel.uiState.value.showApiSettings)
    }

    @Test
    fun `error event is emitted when handling local video fails`() =
        runTest(testDispatcher) {
            val uri = org.mockito.kotlin.mock<android.net.Uri>()
            val errorMsg = "Local video processing failed"
            `when`(
                processLocalVideoUseCase.invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any()),
            ).thenReturn(Result.failure(Exception(errorMsg)))

            val errorEvents = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errorEvents)
                }

            viewModel.handleLocalVideo(uri)
            advanceUntilIdle()

            assertTrue(errorEvents.isNotEmpty())
            assertEquals("导入视频失败: $errorMsg", errorEvents.first())

            job.cancel()
        }

    @Test
    fun `error event is emitted when downloading video fails`() =
        runTest(testDispatcher) {
            val url = "http://example.com/video"
            val errorMsg = "Download failed"
            `when`(
                downloader.downloadVideo(org.mockito.kotlin.any(), org.mockito.kotlin.any()),
            ).thenReturn(Result.failure(Exception(errorMsg)))

            val errorEvents = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errorEvents)
                }

            viewModel.addDownloadTask(url)
            advanceUntilIdle()

            assertTrue(errorEvents.isNotEmpty())
            assertEquals("下载失败：$errorMsg", errorEvents.first())

            job.cancel()
        }

    @Test
    fun `removeTask filters out task`() {
        viewModel.addDownloadTask("http://example.com/video")
        val stateBefore = viewModel.uiState.value
        assertTrue(stateBefore.downloadTasks.isNotEmpty())

        val taskId = stateBefore.downloadTasks.first().id
        viewModel.removeTask(taskId)

        val stateAfter = viewModel.uiState.value
        assertTrue(stateAfter.downloadTasks.isEmpty())
    }

    @Test
    fun `addDownloadTask creates task and starts download`() =
        runTest(testDispatcher) {
            val url = "http://example.com/video"
            `when`(
                downloader.downloadVideo(org.mockito.kotlin.any(), org.mockito.kotlin.any()),
            ).thenReturn(Result.success("path/to/video.mp4"))
            `when`(
                summarizeVideoUseCase.invoke(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                ),
            ).thenReturn(Result.success("Summary text"))

            viewModel.addDownloadTask(url)

            val state = viewModel.uiState.value
            assertEquals(1, state.downloadTasks.size)
            val task = state.downloadTasks.first()
            assertEquals(url, task.url)

            advanceUntilIdle()

            val updatedState = viewModel.uiState.value
            assertEquals("path/to/video.mp4", updatedState.downloadTasks.first().localPath)
        }

    @Test
    fun `cancelDownload sets task status to cancelled`() =
        runTest(testDispatcher) {
            viewModel.addDownloadTask("http://example.com/video")
            val task = viewModel.uiState.value.downloadTasks.first()

            viewModel.cancelDownload(task.id)

            org.mockito.kotlin.verify(downloader).cancelDownload()

            val updatedTask = viewModel.uiState.value.downloadTasks.find { it.id == task.id }
            assertEquals(com.example.summarizer.videosummarizer.services.DownloadStatus.CANCELLED, updatedTask?.progress?.status)
        }

    @Test
    fun `handleLocalVideo adds task and starts summary`() =
        runTest(testDispatcher) {
            val uri = org.mockito.kotlin.mock<android.net.Uri>()
            val mockFile = java.io.File("local/path/video.mp4")
            `when`(
                processLocalVideoUseCase(org.mockito.kotlin.any(), org.mockito.kotlin.any()),
            ).thenReturn(Result.success(Pair("video.mp4", mockFile)))
            `when`(
                summarizeVideoUseCase.invoke(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any(),
                ),
            ).thenReturn(Result.success("Summary text"))

            viewModel.handleLocalVideo(uri)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.downloadTasks.size)
            val task = state.downloadTasks.first()
            assertEquals("video.mp4", task.title)
            assertEquals(mockFile.absolutePath, task.localPath)
        }

    @Test
    fun `addDownloadTask without network shows error`() =
        runTest(testDispatcher) {
            val errors = mutableListOf<String>()
            val job =
                launch {
                    viewModel.errorEvents.toList(errors)
                }
            `when`(networkMonitor.isConnected).thenReturn(MutableStateFlow(false))
            viewModel.addDownloadTask("http://example.com/video")
            advanceUntilIdle()
            assertTrue(errors.contains("当前处于无网络环境，无法下载视频。"))
            job.cancel()
        }
}
