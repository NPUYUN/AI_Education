package com.example.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatUtils {
    const val DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val DATE_FORMAT = "yyyy-MM-dd"

    fun format(
        date: Date,
        pattern: String = DEFAULT_FORMAT,
    ): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    fun format(
        timestamp: Long,
        pattern: String = DEFAULT_FORMAT,
    ): String {
        return format(Date(timestamp), pattern)
    }

    fun getCurrentDate(pattern: String = DEFAULT_FORMAT): String {
        return format(Date(), pattern)
    }

    fun parse(
        dateString: String,
        pattern: String = DEFAULT_FORMAT,
    ): Date? {
        return try {
            SimpleDateFormat(pattern, Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
