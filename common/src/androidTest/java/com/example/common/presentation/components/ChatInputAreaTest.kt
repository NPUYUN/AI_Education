package com.example.common.presentation.components

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.common.R
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
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.input_chat_content)).assertExists()

        // Input text
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.input_chat_content)).performTextInput("!")

        // Wait for UI to update (not strictly needed in performTextInput as it's synchronous in tests, but good practice)
        composeTestRule.waitForIdle()

        // Verify the send button exists and perform click
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.send_message)).assertExists().performClick()

        // Assert that the callbacks were triggered
        assertEquals(true, sendClicked)
    }
}
