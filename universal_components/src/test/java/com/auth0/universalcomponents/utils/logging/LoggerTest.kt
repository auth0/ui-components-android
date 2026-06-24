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
        Logger.setEnabled(false)
        unmockkStatic(Log::class)
    }

    @Test
    fun `d - when disabled - does not call Log`() {
        Logger.setEnabled(false)
        Logger.d(tag, message)
        verify(exactly = 0) { Log.d(any(), any()) }
    }

    @Test
    fun `d - when enabled - delegates to Log`() {
        Logger.setEnabled(true)
        Logger.d(tag, message)
        verify(exactly = 1) { Log.d(tag, message) }
    }

    @Test
    fun `e - when enabled - delegates to Log with throwable`() {
        Logger.setEnabled(true)
        val throwable = RuntimeException("boom")
        Logger.e(tag, message, throwable)
        verify(exactly = 1) { Log.e(tag, message, throwable) }
    }

    @Test
    fun `e - when disabled - does not call Log`() {
        Logger.setEnabled(false)
        Logger.e(tag, message, RuntimeException("boom"))
        verify(exactly = 0) { Log.e(any(), any(), any()) }
    }

    @Test
    fun `w - when enabled - delegates to Log`() {
        Logger.setEnabled(true)
        Logger.w(tag, message)
        verify(exactly = 1) { Log.w(tag, message, null) }
    }

    @Test
    fun `i and v - when enabled - delegate to Log`() {
        Logger.setEnabled(true)
        Logger.i(tag, message)
        Logger.v(tag, message)
        verify(exactly = 1) { Log.i(tag, message) }
        verify(exactly = 1) { Log.v(tag, message) }
    }
}
