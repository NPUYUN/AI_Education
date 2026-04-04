package com.example.ai_education

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ai_education.presentation.SplashScreen
import com.example.ai_education.presentation.navigation.AppNavigation
import com.example.ai_education.ui.theme.Ai_EducationTheme
import com.example.ai_tutor.navigation.AiTutorFeatureApi
import com.example.review.navigation.ReviewFeatureApi
import com.example.solver.navigation.SolverFeatureApi
import com.example.summarizer.navigation.SummarizerFeatureApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var aiTutorFeatureApi: AiTutorFeatureApi

    @Inject lateinit var solverFeatureApi: SolverFeatureApi

    @Inject lateinit var summarizerFeatureApi: SummarizerFeatureApi

    @Inject lateinit var reviewFeatureApi: ReviewFeatureApi

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
                    AppNavigation(
                        aiTutorFeatureApi = aiTutorFeatureApi,
                        solverFeatureApi = solverFeatureApi,
                        summarizerFeatureApi = summarizerFeatureApi,
                        reviewFeatureApi = reviewFeatureApi,
                    )
                }
            }
        }
    }
}
