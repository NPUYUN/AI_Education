package com.example.common.utils

import java.util.regex.Pattern

object StringUtils {
    fun isEmailValid(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return Pattern.compile(emailPattern).matcher(email).matches()
    }

    fun isPasswordValid(password: String): Boolean {
        // At least 8 chars, 1 letter, 1 number
        return password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
    }
}
