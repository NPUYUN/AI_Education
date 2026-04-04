package com.example.common.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeMarkdownTextTest {

    @Test
    fun `normalizeMarkdownForDisplay handles empty string`() {
        val result = normalizeMarkdownForDisplay("")
        assertEquals("暂无内容", result)
        
        val blankResult = normalizeMarkdownForDisplay("   \n  ")
        assertEquals("暂无内容", blankResult)
    }

    @Test
    fun `normalizeMarkdownForDisplay handles inline and block latex math`() {
        val input = "Here is some inline math \\(x = 1\\) and some block math \\[ y = x^2 \\] and standard \$z=3\$."
        val result = normalizeMarkdownForDisplay(input)
        
        // \(...\) -> $$...$$
        assertTrue(result.contains("\$\$x = 1\$\$"))
        // \[...\] -> block $$...$$
        assertTrue(result.contains("\n\$\$\ny = x^2\n\$\$\n"))
        // $...$ -> $$...$$
        assertTrue(result.contains("\$\$z=3\$\$"))
    }

    @Test
    fun `normalizeMarkdownForDisplay removes markdown fence if it wraps entire content`() {
        val input = "```markdown\n# Title\nContent\n```"
        val result = normalizeMarkdownForDisplay(input)
        assertTrue(result.startsWith("# Title"))
        assertTrue(result.contains("Content"))
        // Should not contain the fence anymore
        assertTrue(!result.contains("```markdown"))
    }

    @Test
    fun `normalizeMarkdownForDisplay handles long text and malicious script tags`() {
        val input = "Some text <script>alert('XSS')</script> and more text. " + "A".repeat(1000)
        val result = normalizeMarkdownForDisplay(input)
        // Ensure it doesn't crash on long strings
        assertTrue(result.length > 1000)
        // Markwon handles XSS naturally as it parses markdown to spanned, but let's just ensure our normalizer doesn't mess it up
        assertTrue(result.contains("<script>alert('XSS')</script>"))
    }
}
