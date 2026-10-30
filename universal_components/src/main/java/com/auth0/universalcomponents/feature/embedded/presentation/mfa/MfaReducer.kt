package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

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
                    val wait = outcome.pollInMs ?: phase.everyMs
                    MfaUpdate(step.copy(phase = phase.copy(everyMs = wait)), listOf(MfaCommand.CheckPush(wait)))
                } else {
                    unchanged
                }
            }

            // The host shows the success screen; MFA only says it's done.
            is MfaOutcome.SignedIn -> MfaUpdate(step, effect = MfaEffect.Authenticated(outcome.credentials))

            is MfaOutcome.Failed -> showFailure(step, outcome.failure)
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

    /** Try again repeats the push only if the server kept the session; otherwise the sign-in starts over. */
    private fun showFailure(step: MfaStep, failure: LoginFailure): MfaUpdate {
        val retry = (step as? MfaStep.PushWaiting)?.factor?.takeIf { failure.canRetryStep }
        return MfaUpdate(MfaStep.Failed(failure.error, retry))
    }

    private fun retryFailedStep(failed: MfaStep.Failed): MfaUpdate =
        failed.retry?.let { selectFactor(it) } ?: MfaUpdate(failed, effect = MfaEffect.Restart)

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
    }
}
