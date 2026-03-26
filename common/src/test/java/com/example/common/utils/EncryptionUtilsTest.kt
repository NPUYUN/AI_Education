package com.example.common.utils

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EncryptionUtilsTest {
    @Test
    fun `sha256 generates correct hash`() {
        val input = "hello world"
        val expected = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
        val result = EncryptionUtils.sha256(input)
        assertEquals(expected, result)
    }

    @Test
    fun `encryptAES and decryptAES work symmetrically`() {
        val originalText = "SuperSecretData123"
        val secretKey = "MySuperSecretKey"

        // Mock Base64
        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<String> { Base64.encodeToString(Mockito.any(ByteArray::class.java), Mockito.eq(Base64.DEFAULT)) }
                .thenAnswer { invocation ->
                    val bytes = invocation.arguments[0] as ByteArray
                    java.util.Base64.getEncoder().encodeToString(bytes)
                }

            mockedBase64.`when`<ByteArray> { Base64.decode(Mockito.anyString(), Mockito.eq(Base64.DEFAULT)) }
                .thenAnswer { invocation ->
                    val string = invocation.arguments[0] as String
                    java.util.Base64.getDecoder().decode(string)
                }

            val encrypted = EncryptionUtils.encryptAES(originalText, secretKey)

            val decrypted = EncryptionUtils.decryptAES(encrypted, secretKey)
            assertEquals(originalText, decrypted)
        }
    }

    @Test
    fun `encryptAES with short key handles padding correctly`() {
        val originalText = "Data"
        val shortKey = "Short" // Less than 16 bytes

        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<String> { Base64.encodeToString(Mockito.any(ByteArray::class.java), Mockito.eq(Base64.DEFAULT)) }
                .thenAnswer { invocation ->
                    val bytes = invocation.arguments[0] as ByteArray
                    java.util.Base64.getEncoder().encodeToString(bytes)
                }

            mockedBase64.`when`<ByteArray> { Base64.decode(Mockito.anyString(), Mockito.eq(Base64.DEFAULT)) }
                .thenAnswer { invocation ->
                    val string = invocation.arguments[0] as String
                    java.util.Base64.getDecoder().decode(string)
                }

            val encrypted = EncryptionUtils.encryptAES(originalText, shortKey)
            val decrypted = EncryptionUtils.decryptAES(encrypted, shortKey)

            assertEquals(originalText, decrypted)
        }
    }

    @Test
    fun `encryptAES with long key handles truncation correctly`() {
        val originalText = "Data"
        val longKey = "ThisIsAVeryLongKeyThatExceedsThirtyTwoBytesLength" // > 32 bytes

        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<String> { Base64.encodeToString(Mockito.any(ByteArray::class.java), Mockito.eq(Base64.DEFAULT)) }
                .thenAnswer { invocation ->
                    val bytes = invocation.arguments[0] as ByteArray
                    java.util.Base64.getEncoder().encodeToString(bytes)
                }

            mockedBase64.`when`<ByteArray> { Base64.decode(Mockito.anyString(), Mockito.eq(Base64.DEFAULT)) }
                .thenAnswer { invocation ->
                    val string = invocation.arguments[0] as String
                    java.util.Base64.getDecoder().decode(string)
                }

            val encrypted = EncryptionUtils.encryptAES(originalText, longKey)
            val decrypted = EncryptionUtils.decryptAES(encrypted, longKey)

            assertEquals(originalText, decrypted)
        }
    }

    @Test
    fun `encryptAES handles exceptions and returns empty string`() {
        // Mock Base64 to throw an exception to simulate failure
        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<ByteArray> { Base64.decode(Mockito.anyString(), Mockito.eq(Base64.DEFAULT)) }
                .thenThrow(IllegalArgumentException("Invalid Base64"))

            val invalidEncrypted = "NotBase64!!!"
            val decrypted = EncryptionUtils.decryptAES(invalidEncrypted, "Key")
            assertEquals("", decrypted)
        }
    }
}
