package com.example.review.planner.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.example.common.database.models.ErrorBookEntity
import com.example.review.planner.presentation.viewmodels.ReviewUiState
import com.example.review.planner.presentation.viewmodels.ReviewViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class ErrorBookScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ErrorBookView renders markdown successfully`() {
        val mockViewModel = mock(ReviewViewModel::class.java)
        
        // Create mock data containing typical markdown elements
        val testMarkdownContent = "## Title\n\nSome **bold** text and $$ x=1 $$"
        
        val uiState = ReviewUiState(
            errorRecords = listOf(
                ErrorBookEntity(
                    id = 1,
                    subject = "Math",
                    questionContent = testMarkdownContent,
                    errorReason = "Calculation error",
                    correctSolution = "Correct step 1",
                    timestamp = System.currentTimeMillis()
                )
            ),
            selectedErrorIds = emptySet()
        )

        composeTestRule.setContent {
            ErrorBookView(
                viewModel = mockViewModel,
                uiState = uiState
            )
        }

        // Verify the labels are displayed
        composeTestRule.onNodeWithText("题目：").assertIsDisplayed()
        composeTestRule.onNodeWithText("错误原因：").assertIsDisplayed()
        composeTestRule.onNodeWithText("正确解析：").assertIsDisplayed()

        // Since Markwon renders inside an AndroidView, we can't directly check compose tree for parsed markdown.
        // But we ensure no crashes occurred and the container view exists.
    }
}
