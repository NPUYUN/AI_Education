package com.example.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss"

    fun getCurrentDate(format: String = DEFAULT_FORMAT): String {
        return formatDate(Date(), format)
    }

    fun formatDate(date: Date, format: String = DEFAULT_FORMAT): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(date)
    }

    fun parseDate(dateString: String, format: String = DEFAULT_FORMAT): Date? {
        return try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
