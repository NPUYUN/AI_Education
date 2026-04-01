package com.example.common.ui.components

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
    val currentStyle =
        style ?: MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
        )

    val safeMarkdown =
        remember(markdown) {
            normalizeMarkdownForDisplay(markdown)
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

private fun normalizeMarkdownForDisplay(rawMarkdown: String): String {
    if (rawMarkdown.isBlank()) return "暂无内容"

    var text =
        rawMarkdown
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\u200B", "")
            .replace("\uFEFF", "")

    // Some model responses return JSON-escaped newlines. Convert them only when text has
    // almost no real line breaks, to avoid touching legitimate backslash content.
    if (!text.contains('\n') && text.contains("\\n")) {
        text = text.replace("\\n", "\n")
    }

    text = unwrapOuterMarkdownFence(text).trim()

    val lines = text.lines()
    val normalized = StringBuilder()
    var inCodeBlock = false

    for (line in lines) {
        val trimmedStart = line.trimStart()
        if (trimmedStart.startsWith("```")) {
            inCodeBlock = !inCodeBlock
        }

        val isBlockStart =
            !inCodeBlock &&
                (
                    trimmedStart.startsWith("#") ||
                        trimmedStart.startsWith("- ") ||
                        trimmedStart.startsWith("* ") ||
                        trimmedStart.matches(Regex("^\\d+\\.\\s+.*"))
                )

        if (isBlockStart && normalized.isNotEmpty() && !normalized.endsWith("\n\n")) {
            normalized.append('\n')
        }

        normalized.append(line).append('\n')
    }

    return normalized.toString().trim().ifEmpty { "暂无内容" }
}

private fun unwrapOuterMarkdownFence(input: String): String {
    val trimmed = input.trim()
    if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) return input

    val lines = trimmed.lines()
    if (lines.size < 3) return input

    val firstFence = lines.first().trim()
    val lastFence = lines.last().trim()
    if (lastFence != "```") return input

    val lang = firstFence.removePrefix("```").trim().lowercase()
    val allowedLangHints = setOf("", "markdown", "md", "text", "txt")
    if (lang !in allowedLangHints) return input

    return lines.subList(1, lines.lastIndex).joinToString("\n")
}
