package com.auth0.universalcomponents.utils.logging

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggerTest {

    private val tag = "TestTag"
    private val message = "test message"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `d - delegates to Log`() {
        Logger.d(tag, message)
        verify(exactly = 1) { Log.d(tag, message) }
    }

    @Test
    fun `i - delegates to Log`() {
        Logger.i(tag, message)
        verify(exactly = 1) { Log.i(tag, message) }
    }

    @Test
    fun `v - delegates to Log`() {
        Logger.v(tag, message)
        verify(exactly = 1) { Log.v(tag, message) }
    }

    @Test
    fun `w - delegates to Log with throwable`() {
        val throwable = RuntimeException("boom")
        Logger.w(tag, message, throwable)
        verify(exactly = 1) { Log.w(tag, message, throwable) }
    }

    @Test
    fun `e - delegates to Log with throwable`() {
        val throwable = RuntimeException("boom")
        Logger.e(tag, message, throwable)
        verify(exactly = 1) { Log.e(tag, message, throwable) }
    }
}
