package com.example.summarizer.audio_summarizer.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.R
import com.example.common.ui.components.SafeMarkdownText
import com.example.summarizer.audio_summarizer.presentation.viewmodels.AudioSummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSummaryScreen(
    viewModel: AudioSummaryViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorEvent) {
        errorEvent?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.handleAudioUri(it) }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_summary)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = screenHeight),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Audio Selection Card
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    if (uiState.selectedAudioName.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.selected_audio_name, uiState.selectedAudioName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.audio_file_not_selected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        enabled = !uiState.isTranscribing && !uiState.isSummarizing,
                    ) {
                        Text(
                            if (uiState.selectedAudioUri == null) {
                                stringResource(
                                    R.string.select_audio_file,
                                )
                            } else {
                                stringResource(R.string.reselect)
                            },
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = { viewModel.processAudio() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isTranscribing && !uiState.isSummarizing && uiState.selectedAudioUri != null,
            ) {
                if (uiState.isTranscribing || uiState.isSummarizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.isTranscribing) {
                            stringResource(
                                R.string.extracting_and_transcribing_audio,
                            )
                        } else {
                            stringResource(R.string.generating_smart_summary)
                        },
                    )
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start_processing_and_summarizing))
                }
            }

            // Transcription Result Area
            AnimatedVisibility(
                visible = uiState.transcriptResult.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.audio_transcription_result),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                        Text(
                            text = uiState.transcriptResult,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Summary Result Area
            AnimatedVisibility(
                visible = uiState.summaryResult.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.smart_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val scope = rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        com.example.common.utils.PdfExporter.exportToPdf(context, "音频总结", uiState.summaryResult)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                        SafeMarkdownText(
                            markdown = uiState.summaryResult,
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (uiState.summaryResult.isNotBlank()) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
