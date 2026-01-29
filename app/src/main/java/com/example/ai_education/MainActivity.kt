package com.example.ai_education

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ai_education.ui.theme.Ai_EducationTheme
import com.example.ai_tutor.presentation.navigation.AppNavigation
import com.example.ai_tutor.presentation.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ai_EducationTheme {
                var isSplashVisible by remember { mutableStateOf(true) }
                
                if (isSplashVisible) {
                    SplashScreen(onLoadComplete = {
                        isSplashVisible = false
                    })
                } else {
                    AppNavigation()
                }
            }
        }
    }
}
