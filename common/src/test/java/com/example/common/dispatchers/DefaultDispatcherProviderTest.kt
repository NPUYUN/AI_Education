package com.example.common.dispatchers

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultDispatcherProviderTest {

    @Test
    fun `provides correct dispatchers`() {
        val provider = DefaultDispatcherProvider()
        
        assertEquals(Dispatchers.Main, provider.main)
        assertEquals(Dispatchers.IO, provider.io)
        assertEquals(Dispatchers.Default, provider.default)
        assertEquals(Dispatchers.Unconfined, provider.unconfined)
    }
}
