package com.example.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `isNullOrEmpty returns true for null`() {
        assertTrue(StringUtils.isNullOrEmpty(null))
    }

    @Test
    fun `isNullOrEmpty returns true for empty string`() {
        assertTrue(StringUtils.isNullOrEmpty(""))
    }

    @Test
    fun `isNullOrEmpty returns false for non-empty string`() {
        assertFalse(StringUtils.isNullOrEmpty("hello"))
    }

    @Test
    fun `capitalizeFirstLetter returns empty for empty string`() {
        assertEquals("", StringUtils.capitalizeFirstLetter(""))
    }

    @Test
    fun `capitalizeFirstLetter capitalizes first letter correctly`() {
        assertEquals("Hello", StringUtils.capitalizeFirstLetter("hello"))
        assertEquals("World", StringUtils.capitalizeFirstLetter("World"))
        assertEquals("A", StringUtils.capitalizeFirstLetter("a"))
    }
}
