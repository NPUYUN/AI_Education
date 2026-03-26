package com.example.common.utils

object StringUtils {
    fun isNullOrEmpty(str: String?): Boolean {
        return str.isNullOrEmpty()
    }

    fun capitalizeFirstLetter(str: String): String {
        if (str.isEmpty()) return str
        return str.substring(0, 1).uppercase() + str.substring(1)
    }
}
