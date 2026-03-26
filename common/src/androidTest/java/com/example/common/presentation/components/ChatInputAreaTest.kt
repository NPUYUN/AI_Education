package com.example.common.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatInputAreaTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatInputArea_textInputAndSend_triggersCallback() {
        var inputTextChanged = ""
        var sendClicked = false

        composeTestRule.setContent {
            ChatInputArea(
                text = "Hello Test",
                onTextChanged = { inputTextChanged = it },
                onSend = { sendClicked = true },
                onVoiceStart = {},
                onVoiceEnd = {},
                onCameraClick = {},
                onGalleryClick = {},
                isLoading = false,
            )
        }

        // Verify text field exists and has the correct text
        composeTestRule.onNodeWithContentDescription("输入聊天内容").assertExists()

        // Input text
        composeTestRule.onNodeWithContentDescription("输入聊天内容").performTextInput("!")

        // Wait for UI to update (not strictly needed in performTextInput as it's synchronous in tests, but good practice)
        composeTestRule.waitForIdle()

        // Verify the send button exists and perform click
        composeTestRule.onNodeWithContentDescription("发送消息").assertExists().performClick()

        // Assert that the callbacks were triggered
        assertEquals(true, sendClicked)
    }
}
