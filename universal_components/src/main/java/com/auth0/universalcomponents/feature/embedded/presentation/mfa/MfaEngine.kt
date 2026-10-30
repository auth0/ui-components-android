package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.universalcomponents.domain.network.Result
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus
import com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository
import com.auth0.universalcomponents.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One run of MFA: holds the step on show, runs the work [MfaReducer] asks for, and reports the end through
 * [onEffect]. It makes no decisions itself and knows nothing about the first factor, so any host can own it:
 * embedded login today, a standalone MFA component or step-up later. Its work runs in a child of [hostScope]
 * that [stop] cancels, so the host ends MFA with one call.
 */
internal class MfaEngine(
    factors: List<MfaFactor>,
    private val repository: MfaRepository,
    hostScope: CoroutineScope,
    private val onStep: (MfaStep) -> Unit,
    private val onEffect: (MfaEffect) -> Unit,
) {

    private val reducer = MfaReducer(factors)
    private val job = SupervisorJob(hostScope.coroutineContext[Job])
    private val scope = CoroutineScope(hostScope.coroutineContext + job)

    lateinit var step: MfaStep
        private set

    /** The wait before the next push check. Only the wait: a check that has started is never cut off here. */
    private var waitToCheck: Job? = null

    /** The one-second wait before the resend countdown moves on. */
    private var resendCountdownJob: Job? = null

    init {
        applyUpdate(reducer.start())
    }

    fun onIntent(intent: MfaIntent) = applyUpdate(reducer.handleIntent(step, intent))

    /** Ends this run: nothing it started reaches the host any more. */
    fun stop() = job.cancel()

    private fun onOutcome(outcome: MfaOutcome) = applyUpdate(reducer.handleOutcome(step, outcome))

    /** An effect means MFA is over, so the engine stops itself once it has told the host; no timer outlives it. */
    private fun applyUpdate(update: MfaUpdate) {
        if (!job.isActive) return
        step = update.step
        onStep(update.step)
        update.commands.forEach(::runCommand)
        update.effect?.let { effect ->
            stop()
            onEffect(effect)
        }
    }

    private fun runCommand(command: MfaCommand) {
        when (command) {
            MfaCommand.StopPush -> waitToCheck?.cancel()
            is MfaCommand.CheckPush -> schedulePushCheck(command.afterMs)
            MfaCommand.CountDownResend -> countDownResend()
            // A check still in flight finishes first: the repository runs one SDK step at a time.
            is MfaCommand.SendPush -> {
                waitToCheck?.cancel()
                scope.launch {
                    val sent = repository.sendPush(command.factor)
                    onOutcome(sent.toOutcome(MfaOutcome::SendFailed) { MfaOutcome.PushSent(it) })
                }
            }
        }
    }

    /**
     * Waits, then checks once. Stopping cancels only the wait: a check that has started runs to the end and its
     * answer still comes in, so an approval that lands after the user picked another device isn't lost. The
     * reducer drops answers that no longer fit the screen. Only [stop] ends a check in flight.
     */
    private fun schedulePushCheck(delayMs: Long) {
        waitToCheck?.cancel()
        waitToCheck = scope.launch {
            delay(delayMs)
            scope.launch { onOutcome(checkPush()) }
        }
    }

    private suspend fun checkPush(): MfaOutcome = repository.checkPush().toOutcome(MfaOutcome::CheckFailed) { status ->
        when (status) {
            is PushStatus.Approved -> MfaOutcome.SignedIn(status.credentials)
            is PushStatus.Pending -> MfaOutcome.PushPending(status.retryInMs, status.isSlowDown)
        }
    }

    /** Replaces any tick still waiting, so a new countdown never loses a second to an old one. */
    private fun countDownResend() {
        resendCountdownJob?.cancel()
        resendCountdownJob = scope.launch {
            delay(ONE_SECOND_MILLIS)
            applyUpdate(reducer.handleSecondPassed(step))
        }
    }

    /** A failure is logged once here, with its cause, so it can be traced in a live app. */
    private inline fun <T> Result<T, LoginFailure>.toOutcome(
        onFailure: (LoginFailure) -> MfaOutcome,
        onSuccess: (T) -> MfaOutcome,
    ): MfaOutcome = when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> {
            Logger.e(TAG, "MFA step failed: ${error.error}", error.cause)
            onFailure(error)
        }
    }

    private companion object {
        const val TAG = "MfaEngine"
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
