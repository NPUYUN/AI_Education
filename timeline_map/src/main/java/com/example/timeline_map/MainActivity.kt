package com.example.timeline_map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.timeline_map.ui.theme.Ai_EducationTheme
import com.example.timeline_map.presentation.TimelineMapScreen

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ai_EducationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    TimelineMapScreen()
                }
            }
        }
    }
}
