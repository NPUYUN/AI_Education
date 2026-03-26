package com.example.ai_tutor.multimodal_chat.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysSuggestionsAndHandlesClicks() {
        var clickedSuggestion = ""
        val suggestions = listOf("如何解一元二次方程？", "推荐几本课外阅读书籍", "解释一下牛顿第一定律")

        composeTestRule.setContent {
            WelcomeScreen(
                suggestions = suggestions,
                onSuggestionClick = { clickedSuggestion = it },
            )
        }

        // Verify that the title is displayed
        composeTestRule.onNodeWithText("欢迎使用 AI 辅导").assertExists()
        composeTestRule.onNodeWithText("你可以尝试问我：").assertExists()

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
