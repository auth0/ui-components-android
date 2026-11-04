package com.auth0.universalcomponents.feature.mfachallenge.presentation

import android.os.SystemClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Time that keeps counting while the device sleeps. [TimeSource.Monotonic] and `delay()` both stand still then, so a
 * user who locks the phone for two minutes would come back to a Resend wait that has barely moved.
 */
internal object ElapsedRealtimeSource : TimeSource {

    override fun markNow(): TimeMark = ElapsedRealtimeMark(SystemClock.elapsedRealtime())

    private class ElapsedRealtimeMark(private val startedAtMs: Long) : TimeMark {
        override fun elapsedNow(): Duration = (SystemClock.elapsedRealtime() - startedAtMs).milliseconds
    }
}
