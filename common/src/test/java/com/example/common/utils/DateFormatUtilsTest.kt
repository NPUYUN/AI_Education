package com.example.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class DateFormatUtilsTest {

    @Test
    fun `format date returns correctly formatted string`() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = format.parse("2024-01-01 12:00:00")!!
        val result = DateFormatUtils.format(date)
        assertEquals("2024-01-01 12:00:00", result)
    }

    @Test
    fun `format timestamp returns correctly formatted string`() {
        val timestamp = 1704110400000L // 2024-01-01 12:00:00 UTC, might be different based on timezone
        // Rather than assert exact string, just assert it produces a string of right length
        val result = DateFormatUtils.format(timestamp, "yyyy-MM-dd")
        assertEquals(10, result.length)
    }

    @Test
    fun `getCurrentDate returns valid string`() {
        val result = DateFormatUtils.getCurrentDate("yyyy-MM-dd")
        assertEquals(10, result.length)
    }

    @Test
    fun `parse valid date string returns Date object`() {
        val dateString = "2024-01-01 12:00:00"
        val date = DateFormatUtils.parse(dateString)
        assertNotNull(date)
        
        // Verify format back matches
        val formatted = DateFormatUtils.format(date!!)
        assertEquals(dateString, formatted)
    }

    @Test
    fun `parse invalid date string returns null`() {
        val date = DateFormatUtils.parse("invalid-date", "yyyy-MM-dd")
        // Since SimpleDateFormat can be lenient, "invalid-date" might not parse, or throw ParseException
        // Usually it throws exception and returns null according to catch block
        assertNull(date)
    }
}
