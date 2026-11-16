package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The last code sent, to whom, and when another may be asked for. It outlives the screens: a code asked for from the
 * password screen waits as long as Resend does, so switching between the two can't send one on every tap.
 *
 * The wait is a deadline on [timeSource], not a count of ticks: a screen that stopped redrawing (the device asleep, the
 * process frozen) still reads the time that really passed when it draws again.
 */
internal class CodeCooldown(
    private val timeSource: TimeSource,
    private val wait: Duration = DEFAULT_WAIT,
) {

    private class Sent(val method: LoginOption, val identifier: String, val availableAt: TimeMark)

    private var sent: Sent? = null

    /** A code just went to [identifier]; another may be asked for once the wait is over. */
    fun start(method: LoginOption, identifier: String) {
        sent = Sent(method, identifier, timeSource.markNow() + wait)
    }

    /** The code can't be used any more, so a new one may be asked for at once. */
    fun end(method: LoginOption, identifier: String) {
        if (sent.isFor(method, identifier)) sent = Sent(method, identifier, timeSource.markNow())
    }

    /** Whole seconds, rounded up, until another code may be asked for [identifier]; zero when none was sent to them. */
    fun secondsLeft(method: LoginOption, identifier: String): Int =
        sent?.takeIf { it.isFor(method, identifier) }?.availableAt?.secondsLeft() ?: 0

    /** Whether a wait is still running, so there is something left to count down on screen. */
    val isRunning: Boolean
        get() = (sent?.availableAt?.secondsLeft() ?: 0) > 0

    private fun Sent?.isFor(method: LoginOption, identifier: String) =
        this != null && this.method == method && this.identifier == identifier

    /** The deadline is ahead of now, so the time elapsed since it is negative until it passes. */
    private fun TimeMark.secondsLeft(): Int {
        val left = -elapsedNow()
        return if (left.isPositive()) ((left.inWholeMilliseconds + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt() else 0
    }

    companion object {
        /** The wait before a code can be asked for again, wherever it is asked for. */
        val DEFAULT_WAIT = 60.seconds

        private const val MILLIS_PER_SECOND = 1_000L
    }
}
