package com.example.video_summarizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.video_summarizer.ui.SummaryScreen
import com.example.video_summarizer.ui.theme.EducationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EducationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SummaryScreen()
                }
            }
        }
    }
}
