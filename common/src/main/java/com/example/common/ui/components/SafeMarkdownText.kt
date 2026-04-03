package com.example.common.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TablePlugin

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
    val markwon =
        remember(context) {
            Markwon
                .builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(JLatexMathPlugin.create(48f, 0f))
                .build()
        }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextIsSelectable(true)
                textSize = currentStyle.fontSize.value
            }
        },
        update = { tv ->
            tv.setTextColor(currentStyle.color.toArgb())
            tv.textSize = currentStyle.fontSize.value
            markwon.setMarkdown(tv, safeMarkdown)
        },
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

    // Normalize LaTeX formulas
    // Some models use [ ... ] for block math and \( ... \) for inline math
    // We convert them to standard $...$ and $$...$$ for JLatexMathPlugin
    text =
        text.replace(Regex("""\\\[(.*?)\\\]""", RegexOption.DOT_MATCHES_ALL)) {
            "$$" + it.groupValues[1] + "$$"
        }
    text =
        text.replace(Regex("""\\\((.*?)\\\)""", RegexOption.DOT_MATCHES_ALL)) {
            "$" + it.groupValues[1] + "$"
        }
    // Handle cases where the model outputs `[ R = ... ]` without escaping the bracket
    text =
        text.replace(Regex("""^\s*\[(.*?)\]\s*$""", RegexOption.MULTILINE)) {
            "$$" + it.groupValues[1] + "$$"
        }

    // Some model responses return JSON-escaped newlines. Convert them only when text has
    // almost no real line breaks, to avoid touching legitimate backslash content.
    if (!text.contains('\n') && text.contains("\\n")) {
        text = text.replace("\\n", "\n")
    }

    text = unwrapOuterMarkdownFence(text).trim()
    text = normalizeMathDelimiters(text)

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

private fun normalizeMathDelimiters(input: String): String {
    var text = input

    // Unescape math delimiters frequently produced by JSON/LLM output.
    text = text.replace("\\$", "$")

    // \(...\) -> $...$ (inline math)
    text =
        Regex("""\\\((.+?)\\\)""", setOf(RegexOption.DOT_MATCHES_ALL))
            .replace(text) { match ->
                "$${match.groupValues[1].trim()}$"
            }

    // \[...\] -> $$...$$ (block math)
    text =
        Regex("""\\\[(.+?)\\\]""", setOf(RegexOption.DOT_MATCHES_ALL))
            .replace(text) { match ->
                "\n$$\n${match.groupValues[1].trim()}\n$$\n"
            }

    // Normalize "$ expr $" into "$expr$" for better parser compatibility.
    text =
        Regex("""\$\s+([^$\n][^$]*?)\s+\$""")
            .replace(text) { match ->
                "$${match.groupValues[1].trim()}$"
            }

    // Normalize "$$ expr $$" into canonical block form.
    text =
        Regex("""\$\$\s+(.+?)\s+\$\$""", setOf(RegexOption.DOT_MATCHES_ALL))
            .replace(text) { match ->
                "\n$$\n${match.groupValues[1].trim()}\n$$\n"
            }

    return text
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
