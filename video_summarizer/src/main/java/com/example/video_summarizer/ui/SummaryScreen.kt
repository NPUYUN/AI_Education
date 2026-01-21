package com.example.video_summarizer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.video_summarizer.processor.VideoProcessor
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.example.video_summarizer.R

@Composable
fun SummaryScreen() {
    val processor = remember { VideoProcessor() }
    val noVideoProcessed = stringResource(R.string.no_video_processed)
    var summary by remember { mutableStateOf(noVideoProcessed) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.title_video_summarizer))
        
        Button(onClick = { 
            scope.launch {
                // Mock path
                summary = processor.generateSummary("/sdcard/test_video.mp4")
            }
        }) {
            Text(stringResource(R.string.select_summarize_button))
        }

        Text(text = summary, modifier = Modifier.padding(top = 16.dp))
    }
}
