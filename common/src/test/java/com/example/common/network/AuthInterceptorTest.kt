package com.example.common.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class AuthInterceptorTest {

    @Test
    fun `intercept adds correct headers`() {
        val apiKey = "test-api-key"
        val interceptor = AuthInterceptor(apiKey)
        
        val originalRequest = Request.Builder()
            .url("https://api.example.com")
            .build()
            
        val chain = mock(Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(originalRequest)
        
        // Capture the modified request
        var capturedRequest: Request? = null
        `when`(chain.proceed(any())).thenAnswer { invocation ->
            capturedRequest = invocation.arguments[0] as Request
            Response.Builder()
                .request(capturedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }
        
        interceptor.intercept(chain)
        
        assertEquals("Bearer test-api-key", capturedRequest?.header("Authorization"))
        assertEquals("application/json", capturedRequest?.header("Content-Type"))
    }
}
