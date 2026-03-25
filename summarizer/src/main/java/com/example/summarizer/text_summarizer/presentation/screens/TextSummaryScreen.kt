package com.example.summarizer.text_summarizer.presentation.screens

import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.summarizer.text_summarizer.presentation.viewmodels.TextSummaryViewModel
import com.example.common.ui.components.SafeMarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSummaryScreen(
    viewModel: TextSummaryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.handleFileUri(it) }
    }

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .heightIn(min = screenHeight),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Input Area
        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = { viewModel.updateInputText(it) },
            label = { Text("输入需要总结的文本") },
            placeholder = { Text("支持长文本粘贴，或通过下方按钮导入文件...") },
            modifier = Modifier
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
            }
        )

        // File Selection & Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSummarizing && !uiState.isExtractingFile
            ) {
                if (uiState.isExtractingFile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("解析中...")
                } else {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入文件")
                }
            }

            Button(
                onClick = { viewModel.summarize() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSummarizing && !uiState.isExtractingFile && uiState.inputText.isNotBlank()
            ) {
                if (uiState.isSummarizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成中...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("智能总结")
                }
            }
        }

        // Error Message
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.error?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Result Area
        AnimatedVisibility(
            visible = uiState.summaryResult.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "总结结果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    
                    SafeMarkdownText(
                        markdown = uiState.summaryResult,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (uiState.summaryResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
