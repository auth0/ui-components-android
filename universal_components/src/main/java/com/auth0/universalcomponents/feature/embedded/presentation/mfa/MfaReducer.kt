package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushPolling

/**
 * Every MFA rule in one place: the step on show plus an input gives the next step, maybe with work to run and an
 * effect for the host. [factors] are what the server offered, fixed for one run of MFA. Pure, so it needs no
 * Android, coroutines or SDK to test.
 */
internal class MfaReducer(private val factors: List<MfaFactor>) {

    /** The first step, worked out from the factors alone: one factor goes straight to it, several show the list. */
    fun start(): MfaUpdate = when (factors.size) {
        0 -> MfaUpdate(MfaStep.Failed(EmbeddedLoginError.ExtraStepUnsupported))
        1 -> selectFactor(factors.single())
        else -> MfaUpdate(MfaStep.AuthenticatorList(factors))
    }

    fun handleIntent(step: MfaStep, intent: MfaIntent): MfaUpdate {
        val unchanged = MfaUpdate(step)
        return when (intent) {
            is MfaIntent.SelectFactor -> if (step is MfaStep.AuthenticatorList) {
                selectFactor(intent.factor)
            } else {
                unchanged
            }

            MfaIntent.ChooseAnotherFactor -> if (step is MfaStep.PushWaiting && step.canChooseAnother && step.isOut) {
                showList()
            } else {
                unchanged
            }

            MfaIntent.Back -> goBack(step)

            MfaIntent.Retry -> if (step is MfaStep.Failed) retryFailedStep(step) else unchanged

            MfaIntent.Resend -> if (step is MfaStep.PushWaiting && step.canResend) {
                selectFactor(step.factor)
            } else {
                unchanged
            }
        }
    }

    fun handleOutcome(step: MfaStep, outcome: MfaOutcome): MfaUpdate {
        val unchanged = MfaUpdate(step)
        return when (outcome) {
            // Every push sent, the first or a resend, starts the checks and the resend wait.
            is MfaOutcome.PushSent -> if (step is MfaStep.PushWaiting && step.phase == PushPhase.Sending) {
                MfaUpdate(
                    step.copy(phase = PushPhase.Polling(outcome.pollInMs, RESEND_COOLDOWN_SECONDS)),
                    listOf(MfaCommand.CheckPush(outcome.pollInMs), MfaCommand.CountDownResend),
                )
            } else {
                unchanged
            }

            // The user may have left the push screen since; then the check just stops.
            is MfaOutcome.PushPending -> {
                val phase = (step as? MfaStep.PushWaiting)?.phase
                if (step is MfaStep.PushWaiting && phase is PushPhase.Polling) {
                    val wait = outcome.pollInMs ?: if (outcome.isSlowDown) phase.everyMs.backedOff(1) else phase.everyMs
                    MfaUpdate(
                        step.copy(phase = phase.copy(everyMs = wait, failedChecks = 0)),
                        listOf(MfaCommand.CheckPush(wait)),
                    )
                } else {
                    unchanged
                }
            }

            // Even after the user moved on: they did approve, so they're signed in. The host shows the success screen.
            is MfaOutcome.SignedIn -> MfaUpdate(step, effect = MfaEffect.Authenticated(outcome.credentials))

            is MfaOutcome.SendFailed -> if (step is MfaStep.PushWaiting && step.phase == PushPhase.Sending) {
                val retry = MfaRetry.SendAgain(step.factor).takeIf { outcome.failure.canRetryStep }
                MfaUpdate(MfaStep.Failed(outcome.failure.error, retry))
            } else {
                unchanged
            }

            // A check the user has left behind can't fail their next step, so it's only handled on the push screen.
            is MfaOutcome.CheckFailed -> {
                val phase = (step as? MfaStep.PushWaiting)?.phase
                if (step is MfaStep.PushWaiting && phase is PushPhase.Polling) {
                    checkFailed(step, phase, outcome.failure)
                } else {
                    unchanged
                }
            }
        }
    }

    /** One second of the resend wait has passed; it stops at zero, or once the push screen is left. */
    fun handleSecondPassed(step: MfaStep): MfaUpdate {
        if (step !is MfaStep.PushWaiting) return MfaUpdate(step)
        val phase = step.phase
        if (phase !is PushPhase.Polling || phase.resendSecondsLeft == 0) return MfaUpdate(step)
        val left = phase.resendSecondsLeft - 1
        return MfaUpdate(
            step.copy(phase = phase.copy(resendSecondsLeft = left)),
            if (left > 0) listOf(MfaCommand.CountDownResend) else emptyList(),
        )
    }

    /** The push screen shows at once, while the push is sent. */
    private fun selectFactor(factor: MfaFactor): MfaUpdate = when (factor) {
        is MfaFactor.Push -> MfaUpdate(
            MfaStep.PushWaiting(factor, PushPhase.Sending, canChooseAnother = factors.size > 1),
            listOf(MfaCommand.SendPush(factor)),
        )
    }

    private fun showList(): MfaUpdate = MfaUpdate(MfaStep.AuthenticatorList(factors), listOf(MfaCommand.StopPush))

    /**
     * A network blip or server outage on a check keeps the session, so the push the user may already have approved
     * is checked again quietly, waiting longer each time. Only after [QUIET_RETRIES] of those in a row, or any other
     * error, does the error screen show; its Try again then checks once more, without sending a second push.
     */
    private fun checkFailed(step: MfaStep.PushWaiting, phase: PushPhase.Polling, failure: LoginFailure): MfaUpdate {
        if (failure.canRetryStep && phase.failedChecks < QUIET_RETRIES) {
            val failedChecks = phase.failedChecks + 1
            val wait = phase.everyMs.backedOff(failedChecks)
            return MfaUpdate(
                step.copy(phase = phase.copy(failedChecks = failedChecks)),
                listOf(MfaCommand.CheckPush(wait)),
            )
        }
        val retry = MfaRetry.CheckAgain(step.copy(phase = phase.copy(failedChecks = 0)))
        return MfaUpdate(MfaStep.Failed(failure.error, retry.takeIf { failure.canRetryStep }))
    }

    private fun retryFailedStep(failed: MfaStep.Failed): MfaUpdate = when (val retry = failed.retry) {
        is MfaRetry.SendAgain -> selectFactor(retry.factor)
        is MfaRetry.CheckAgain -> checkAgain(retry.waiting)
        null -> MfaUpdate(failed, effect = MfaEffect.Restart)
    }

    /** Back on the push screen, checked at once; the resend wait carries on from where it stopped. */
    private fun checkAgain(waiting: MfaStep.PushWaiting): MfaUpdate {
        val resendLeft = (waiting.phase as? PushPhase.Polling)?.resendSecondsLeft ?: 0
        val countDown = if (resendLeft > 0) listOf(MfaCommand.CountDownResend) else emptyList()
        return MfaUpdate(waiting, listOf(MfaCommand.CheckPush(0)) + countDown)
    }

    /** Doubles the wait [times] times, but never past [PushPolling.MAX_WAIT_MS]. */
    private fun Long.backedOff(times: Int): Long = (this shl times).coerceAtMost(PushPolling.MAX_WAIT_MS)

    /**
     * The server can't step back, so Back stays on our side: from the push to the list when there's a choice,
     * otherwise out of MFA to start over. It waits while the push is being sent.
     */
    private fun goBack(step: MfaStep): MfaUpdate = when (step) {
        is MfaStep.PushWaiting -> when {
            !step.isOut -> MfaUpdate(step)
            step.canChooseAnother -> showList()
            else -> MfaUpdate(step, listOf(MfaCommand.StopPush), MfaEffect.Restart)
        }

        is MfaStep.AuthenticatorList, is MfaStep.Failed -> MfaUpdate(step, effect = MfaEffect.Restart)
    }

    /** The push has gone out, so leaving can't drop it halfway. */
    private val MfaStep.PushWaiting.isOut: Boolean
        get() = phase is PushPhase.Polling

    /** The push is out and its resend wait is over. */
    private val MfaStep.PushWaiting.canResend: Boolean
        get() = (phase as? PushPhase.Polling)?.resendSecondsLeft == 0

    companion object {
        /** The same wait as resending an emailed code. */
        const val RESEND_COOLDOWN_SECONDS = 60

        /** Checks in a row that may fail on the network before the user is told, as on iOS. */
        const val QUIET_RETRIES = 3
    }
}
