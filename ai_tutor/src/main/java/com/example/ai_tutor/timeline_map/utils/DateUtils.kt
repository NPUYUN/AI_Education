package com.example.ai_tutor.timeline_map.utils

object DateUtils {
    fun extractYear(time: String): Int? {
        val isBC = time.contains("前") || time.contains("BC", ignoreCase = true)
        // Matches: 2023年, 2023-01, 2023/01, 2023, 1911.10.10
        val yearRegex = Regex("(\\d{1,4})\\s*(年|-|/|\\.|$)")
        val match = yearRegex.find(time)
        val yearStr = match?.groupValues?.get(1) ?: return null
        var year = yearStr.toIntOrNull() ?: return null
        if (isBC) {
            year = -year
        }
        return year
    }

    fun parseDateKey(time: String): Long {
        val year = extractYear(time) ?: return Long.MAX_VALUE
        
        val monthRegex = Regex("[-/\\.年]\\s*(\\d{1,2})\\s*(月|-|/|\\.|$)")
        val dayRegex = Regex("[-/\\.月]\\s*(\\d{1,2})\\s*(日|号|$)")

        val monthMatch = monthRegex.find(time)
        val dayMatch = dayRegex.find(time)

        val month = monthMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
        val day = dayMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 28) ?: 1
        
        return year.toLong() * 10000 + month * 100 + day
    }
}
