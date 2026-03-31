package com.example.ai_tutor.multimodal_chat.presentation.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.common.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysSuggestionsAndHandlesClicks() {
        var clickedSuggestion = ""
        val suggestions =
            listOf(
                stringResource(R.string.how_to_solve_quadratic_equation),
                stringResource(R.string.recommend_extracurricular_books),
                stringResource(R.string.explain_newtons_first_law),
            )

        composeTestRule.setContent {
            WelcomeScreen(
                suggestions = suggestions,
                onSuggestionClick = { clickedSuggestion = it },
            )
        }

        // Verify that the title is displayed
        composeTestRule.onNodeWithText(stringResource(R.string.welcome_to_ai_tutor)).assertExists()
        composeTestRule.onNodeWithText(stringResource(R.string.you_can_try_asking_me)).assertExists()

        // Verify that all suggestions are displayed
        suggestions.forEach { suggestion ->
            composeTestRule.onNodeWithText(suggestion).assertExists()
        }

        // Perform click on the first suggestion
        composeTestRule.onNodeWithText(suggestions[0]).performClick()

        // Verify that the click callback was triggered with the correct text
        assertEquals(suggestions[0], clickedSuggestion)
    }
}
