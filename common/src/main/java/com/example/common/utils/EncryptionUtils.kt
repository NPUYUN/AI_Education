package com.example.common.utils

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.nio.charset.StandardCharsets

object EncryptionUtils {
    
    fun sha256(input: String): String {
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private const val ALGORITHM = "AES"
    
    // Note: In a real app, do not hardcode keys or use simple ECB mode. 
    // This is a basic utility for demonstration as per requirements.
    
    fun encryptAES(value: String, secretKey: String): String {
        try {
            // Ensure key is 16/24/32 bytes. Truncate or pad if necessary for this simple util.
            val keyBytes = normalizeKey(secretKey)
            val key = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            return Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun decryptAES(value: String, secretKey: String): String {
         try {
            val keyBytes = normalizeKey(secretKey)
            val key = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decoded = Base64.decode(value, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            return String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
    
    private fun normalizeKey(key: String): ByteArray {
        val bytes = key.toByteArray(StandardCharsets.UTF_8)
        return when {
            bytes.size >= 32 -> bytes.copyOf(32)
            bytes.size >= 24 -> bytes.copyOf(24)
            bytes.size >= 16 -> bytes.copyOf(16)
            else -> bytes.copyOf(16) // Pad with zeros
        }
    }
}
