package com.example.common.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import coil.imageLoader
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun SafeMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
) {
    val context = LocalContext.current
    val currentStyle = style ?: MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
    )

    // Basic sanitization: Sometimes LLMs return markdown with leading/trailing spaces or unclosed tags
    // that might crash the renderer.
    val safeMarkdown =
        remember(markdown) {
            var text = markdown.trim()
            // Ensure lists are properly formatted with newlines to avoid rendering issues
            text = text.replace(Regex("([^\n])\n(-|\\*) "), "$1\n\n$2 ")
            text = text.replace(Regex("([^\n])\n(\\d+\\.) "), "$1\n\n$2 ")
            // Also ensure markdown blocks like headers have preceding empty lines if they don't
            text = text.replace(Regex("([^\n])\n(#+) "), "$1\n\n$2 ")
            // Simple fallback if text is somehow empty
            if (text.isEmpty()) {
                text = "暂无内容"
            }
            text
        }

    MarkdownText(
        markdown = safeMarkdown,
        color = currentStyle.color,
        style = currentStyle,
        modifier = modifier,
        isTextSelectable = true,
        disableLinkMovementMethod = true, // Prevents crashes from malformed links
        imageLoader = context.imageLoader,
    )
}
