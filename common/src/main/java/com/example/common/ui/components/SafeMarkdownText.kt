package com.example.common.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun SafeMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    // Basic sanitization: Sometimes LLMs return markdown with leading/trailing spaces or unclosed tags
    // that might crash the renderer.
    val safeMarkdown = remember(markdown) {
        var text = markdown.trim()
        // Simple fallback if text is somehow empty
        if (text.isEmpty()) {
            text = "暂无内容"
        }
        text
    }

    MarkdownText(
        markdown = safeMarkdown,
        style = style,
        modifier = modifier.fillMaxWidth(),
        isTextSelectable = true,
        disableLinkMovementMethod = true // Prevents crashes from malformed links
    )
}
