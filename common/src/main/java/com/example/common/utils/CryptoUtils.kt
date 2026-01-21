package com.example.common.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES"
    
    // Note: In a real production app, the key should be stored in Android Keystore,
    // not hardcoded or derived simply like this. This is a simplified example.
    
    fun encrypt(input: String, key: String): String {
        val secretKey = generateKey(key)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(input: String, key: String): String {
        val secretKey = generateKey(key)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decodedBytes = Base64.decode(input, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }

    private fun generateKey(key: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = key.toByteArray(Charsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        val keyBytes = digest.digest()
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
