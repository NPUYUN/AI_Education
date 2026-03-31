package com.example.summarizer.text_summarizer.presentation.screens

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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.R
import com.example.common.ui.components.SafeMarkdownText
import com.example.summarizer.text_summarizer.presentation.viewmodels.TextSummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSummaryScreen(viewModel: TextSummaryViewModel) {
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

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.handleFileUri(it) }
        }

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
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
            // Input Area
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                label = { Text(stringResource(R.string.input_text_to_summarize)) },
                placeholder = { Text(stringResource(R.string.support_long_text_paste_or_import)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 300.dp),
                maxLines = 15,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (uiState.inputText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateInputText("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                },
            )

            // File Selection & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSummarizing && !uiState.isExtractingFile,
                ) {
                    if (uiState.isExtractingFile) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.parsing_dots))
                    } else {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.import_file))
                    }
                }

                Button(
                    onClick = { viewModel.summarize() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSummarizing && !uiState.isExtractingFile && uiState.inputText.isNotBlank(),
                ) {
                    if (uiState.isSummarizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.generating))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.smart_summary))
                    }
                }
            }

            // Result Area
            AnimatedVisibility(
                visible = uiState.summaryResult.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                                text = stringResource(R.string.summary_result),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val scope = androidx.compose.runtime.rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        com.example.common.utils.PdfExporter.exportToPdf(context, "文本总结", uiState.summaryResult)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                        SafeMarkdownText(
                            markdown = uiState.summaryResult,
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
