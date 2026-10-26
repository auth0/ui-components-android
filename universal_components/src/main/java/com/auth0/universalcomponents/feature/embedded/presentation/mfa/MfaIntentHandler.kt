package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

/**
 * Every MFA rule in one place: old state plus an intent or a result gives the new state, maybe with work to run
 * and an outcome for the host. Pure, so it needs no Android, coroutines or SDK to test.
 */
internal object MfaIntentHandler {

    /** The same wait as resending an emailed code. */
    const val RESEND_COOLDOWN_SECONDS = 5

    fun handleIntent(state: MfaState, intent: MfaIntent): MfaUpdate {
        val step = state.step
        val unchanged = MfaUpdate(state)
        return when (intent) {
            MfaIntent.Start -> if (state.isStarted) unchanged else showFirstStep(state.copy(isStarted = true))

            is MfaIntent.SelectFactor -> if (step == MfaStep.ChooseFactor) {
                selectFactor(state, intent.factor)
            } else {
                unchanged
            }

            MfaIntent.Back -> goBack(state)

            MfaIntent.Retry -> if (step is MfaStep.Failed) retryFailedStep(state, step) else unchanged

            MfaIntent.Resend -> if (step is MfaStep.WaitForPush && step.canResend) {
                selectFactor(state, step.factor)
            } else {
                unchanged
            }
        }
    }

    fun handleResult(state: MfaState, result: MfaResult): MfaUpdate {
        val step = state.step
        val unchanged = MfaUpdate(state)
        return when (result) {
            // Every push sent, the first or a resend, starts the checks and the resend wait.
            is MfaResult.PushSent -> if (step is MfaStep.WaitForPush && step.isSending) {
                MfaUpdate(
                    state.withStep(
                        step.copy(
                            pollInMs = result.pollInMs,
                            isSending = false,
                            resendSecondsLeft = RESEND_COOLDOWN_SECONDS,
                        ),
                    ),
                    listOf(MfaCommand.CheckPush(result.pollInMs), MfaCommand.CountDownResend),
                )
            } else {
                unchanged
            }

            // The user may have left the push screen since; then the check just stops.
            is MfaResult.PushPending -> if (step is MfaStep.WaitForPush && !step.isSending) {
                val wait = result.pollInMs ?: step.pollInMs
                MfaUpdate(state.withStep(step.copy(pollInMs = wait)), listOf(MfaCommand.CheckPush(wait)))
            } else {
                unchanged
            }

            is MfaResult.SignedIn -> MfaUpdate(
                state.withStep(MfaStep.Authenticated),
                effect = MfaEffect.Authenticated(result.credentials),
            )

            is MfaResult.Failed -> showFailure(state, result.failure)
        }
    }

    /** One second of the resend wait has passed; it stops at zero, or once the push screen is left. */
    fun handleSecondPassed(state: MfaState): MfaUpdate {
        val step = state.step
        if (step !is MfaStep.WaitForPush || step.isSending || step.resendSecondsLeft == 0) return MfaUpdate(state)
        val left = step.resendSecondsLeft - 1
        return MfaUpdate(
            state.withStep(step.copy(resendSecondsLeft = left)),
            if (left > 0) listOf(MfaCommand.CountDownResend) else emptyList(),
        )
    }

    /** One factor goes straight to it; several show the list first. */
    private fun showFirstStep(state: MfaState): MfaUpdate = when (state.factors.size) {
        0 -> MfaUpdate(state.withStep(MfaStep.Failed(EmbeddedLoginError.ExtraStepUnsupported)))
        1 -> selectFactor(state, state.factors.single())
        else -> MfaUpdate(state.withStep(MfaStep.ChooseFactor))
    }

    /** The push screen shows at once, while the push is sent. */
    private fun selectFactor(state: MfaState, factor: MfaFactor): MfaUpdate = when (factor) {
        is MfaFactor.Push -> MfaUpdate(
            state.withStep(MfaStep.WaitForPush(factor, isSending = true)),
            listOf(MfaCommand.SendPush(factor)),
        )
    }

    /** Try again repeats the push only if the server kept the session; otherwise the sign-in starts over. */
    private fun showFailure(state: MfaState, failure: LoginFailure): MfaUpdate {
        val retry = (state.step as? MfaStep.WaitForPush)?.factor?.takeIf { failure.canRetryStep }
        return MfaUpdate(state.withStep(MfaStep.Failed(failure.error, retry)))
    }

    private fun retryFailedStep(state: MfaState, failed: MfaStep.Failed): MfaUpdate =
        failed.retry?.let { selectFactor(state, it) } ?: MfaUpdate(state, effect = MfaEffect.Restart)

    /**
     * The server can't step back, so Back stays on our side: from the push to the list when there's a choice,
     * otherwise out of MFA to start over. It waits while the push is being sent.
     */
    private fun goBack(state: MfaState): MfaUpdate = when (val step = state.step) {
        is MfaStep.WaitForPush -> when {
            step.isSending -> MfaUpdate(state)
            state.factors.size > 1 -> MfaUpdate(state.withStep(MfaStep.ChooseFactor), listOf(MfaCommand.StopPush))
            else -> MfaUpdate(state, listOf(MfaCommand.StopPush), MfaEffect.Restart)
        }

        MfaStep.ChooseFactor, is MfaStep.Failed -> MfaUpdate(state, effect = MfaEffect.Restart)

        MfaStep.Loading, MfaStep.Authenticated -> MfaUpdate(state)
    }

    /** The push is out and its resend wait is over. */
    private val MfaStep.WaitForPush.canResend: Boolean
        get() = !isSending && resendSecondsLeft == 0

    private fun MfaState.withStep(step: MfaStep): MfaState = copy(step = step)
}
