package com.example.common.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class RetrofitClientTest {

    @Before
    fun setup() {
        RetrofitClient.clearCache()
    }

    @Test
    fun `create returns a Retrofit instance`() {
        val retrofit = RetrofitClient.create("test-key", "https://api.example.com/")
        assertNotNull(retrofit)
        assertEquals("https://api.example.com/", retrofit.baseUrl().toString())
    }

    @Test
    fun `create caches instances with same key and url`() {
        val retrofit1 = RetrofitClient.create("test-key", "https://api.example.com/")
        val retrofit2 = RetrofitClient.create("test-key", "https://api.example.com/")
        
        assertSame(retrofit1, retrofit2)
    }

    @Test
    fun `create does not cache instances with different keys`() {
        val retrofit1 = RetrofitClient.create("test-key-1", "https://api.example.com/")
        val retrofit2 = RetrofitClient.create("test-key-2", "https://api.example.com/")
        
        assertNotEquals(retrofit1, retrofit2)
    }

    @Test
    fun `clearCache removes all cached instances`() {
        val retrofit1 = RetrofitClient.create("test-key", "https://api.example.com/")
        RetrofitClient.clearCache()
        val retrofit2 = RetrofitClient.create("test-key", "https://api.example.com/")
        
        assertNotEquals(retrofit1, retrofit2)
    }
}
