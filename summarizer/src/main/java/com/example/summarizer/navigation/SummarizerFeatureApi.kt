package com.example.summarizer.navigation

import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.common.navigation.FeatureApi
import com.example.summarizer.audio_summarizer.presentation.screens.AudioSummaryScreen
import com.example.summarizer.audio_summarizer.presentation.viewmodels.AudioSummaryViewModel
import com.example.summarizer.core.presentation.screens.SummaryMenuScreen
import com.example.summarizer.dialogue_summarizer.presentation.screens.DialogueSummaryScreen
import com.example.summarizer.dialogue_summarizer.presentation.viewmodels.DialogueSummaryViewModel
import com.example.summarizer.knowledge_cards.presentation.screens.KnowledgeCardScreen
import com.example.summarizer.knowledge_cards.presentation.viewmodels.KnowledgeCardViewModel
import com.example.summarizer.text_summarizer.presentation.screens.TextSummaryScreen
import com.example.summarizer.text_summarizer.presentation.viewmodels.TextSummaryViewModel
import com.example.summarizer.videosummarizer.presentation.screens.VideoDownloadScreen
import com.example.summarizer.videosummarizer.presentation.viewmodels.VideoDownloadViewModel
import javax.inject.Inject

class SummarizerFeatureApi
    @Inject
    constructor() : FeatureApi {
        override fun registerGraph(
            navGraphBuilder: NavGraphBuilder,
            navController: NavHostController,
            modifier: Modifier,
            onNavigateToCamera: (String) -> Unit,
            outerSavedStateHandle: androidx.lifecycle.SavedStateHandle?,
            sharedViewModel: Any?,
        ) {
            navGraphBuilder.composable("summary") {
                SummaryMenuScreen(
                    onNavigateToVideoSummary = { navController.navigate("video") },
                    onNavigateToTextSummary = { navController.navigate("text_summary") },
                    onNavigateToAudioSummary = { navController.navigate("audio_summary") },
                    onNavigateToChatSummary = { navController.navigate("dialogue_summary") },
                    onNavigateToKnowledgeCards = { navController.navigate("knowledge_cards") },
                )
            }

            navGraphBuilder.composable("video") {
                val videoViewModel: VideoDownloadViewModel = hiltViewModel()
                VideoDownloadScreen(videoViewModel)
            }

            navGraphBuilder.composable("text_summary") {
                val textSummaryViewModel: TextSummaryViewModel = hiltViewModel()
                TextSummaryScreen(viewModel = textSummaryViewModel)
            }

            navGraphBuilder.composable("audio_summary") {
                val audioSummaryViewModel: AudioSummaryViewModel = hiltViewModel()
                AudioSummaryScreen(
                    viewModel = audioSummaryViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            navGraphBuilder.composable("dialogue_summary") {
                val dialogueSummaryViewModel: DialogueSummaryViewModel = hiltViewModel()
                DialogueSummaryScreen(
                    viewModel = dialogueSummaryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            navGraphBuilder.composable("knowledge_cards") {
                val knowledgeCardViewModel: KnowledgeCardViewModel = hiltViewModel()
                KnowledgeCardScreen(
                    viewModel = knowledgeCardViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
