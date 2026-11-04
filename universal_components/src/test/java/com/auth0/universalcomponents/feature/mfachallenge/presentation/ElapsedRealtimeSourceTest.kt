package com.auth0.universalcomponents.feature.mfachallenge.presentation

import android.os.SystemClock
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class ElapsedRealtimeSourceTest {

    @After
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `elapsed - the device slept in between - counts the sleep too`() {
        mockkStatic(SystemClock::class)
        // The phone was locked for two minutes between the mark and the read.
        every { SystemClock.elapsedRealtime() } returnsMany listOf(1_000L, 121_000L)

        val mark = ElapsedRealtimeSource.markNow()

        assertThat(mark.elapsedNow()).isEqualTo(2.minutes)
    }
}
