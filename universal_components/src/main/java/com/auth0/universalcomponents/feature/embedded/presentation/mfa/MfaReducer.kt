package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

/**
 * Every MFA rule in one place: old state plus an intent or a result gives the new state, maybe with work to run
 * and an outcome for the host. Pure, so it needs no Android, coroutines or SDK to test.
 */
internal object MfaReducer {

    fun reduce(state: MfaState, intent: MfaIntent): MfaUpdate {
        val step = state.step
        val unchanged = MfaUpdate(state)
        return when (intent) {
            MfaIntent.Start -> if (state.isStarted) unchanged else start(state.copy(isStarted = true))

            is MfaIntent.SelectFactor -> if (step == MfaStep.ChooseFactor) select(state, intent.factor) else unchanged

            MfaIntent.Back -> back(state)

            MfaIntent.Retry -> if (step is MfaStep.Failed) retry(state, step) else unchanged
        }
    }

    fun reduce(state: MfaState, result: MfaResult): MfaUpdate {
        val step = state.step
        val unchanged = MfaUpdate(state)
        return when (result) {
            is MfaResult.PushSent -> if (step is MfaStep.WaitForPush && step.isSending) {
                MfaUpdate(
                    state.on(step.copy(pollInMs = result.pollInMs, isSending = false)),
                    MfaCommand.CheckPush(result.pollInMs),
                )
            } else {
                unchanged
            }

            // The user may have left the push screen since; then the check just stops.
            is MfaResult.PushPending -> if (step is MfaStep.WaitForPush && !step.isSending) {
                val wait = result.pollInMs ?: step.pollInMs
                MfaUpdate(state.on(step.copy(pollInMs = wait)), MfaCommand.CheckPush(wait))
            } else {
                unchanged
            }

            is MfaResult.SignedIn -> MfaUpdate(
                state.on(MfaStep.Authenticated),
                effect = MfaEffect.Authenticated(result.credentials),
            )

            is MfaResult.Failed -> failed(state, result.failure)
        }
    }

    /** One factor goes straight to it; several show the list first. */
    private fun start(state: MfaState): MfaUpdate = when (state.factors.size) {
        0 -> MfaUpdate(state.on(MfaStep.Failed(EmbeddedLoginError.ExtraStepUnsupported)))
        1 -> select(state, state.factors.single())
        else -> MfaUpdate(state.on(MfaStep.ChooseFactor))
    }

    /** The push screen shows at once, while the push is sent. */
    private fun select(state: MfaState, factor: MfaFactor): MfaUpdate = when (factor) {
        is MfaFactor.Push ->
            MfaUpdate(state.on(MfaStep.WaitForPush(factor, isSending = true)), MfaCommand.SendPush(factor))
    }

    /** Try again repeats the push only if the server kept the session; otherwise the sign-in starts over. */
    private fun failed(state: MfaState, failure: LoginFailure): MfaUpdate {
        val retry = (state.step as? MfaStep.WaitForPush)?.factor?.takeIf { failure.canRetryStep }
        return MfaUpdate(state.on(MfaStep.Failed(failure.error, retry)))
    }

    private fun retry(state: MfaState, failed: MfaStep.Failed): MfaUpdate =
        failed.retry?.let { select(state, it) } ?: MfaUpdate(state, effect = MfaEffect.Restart)

    /**
     * The server can't step back, so Back stays on our side: from the push to the list when there's a choice,
     * otherwise out of MFA to start over. It waits while the push is being sent.
     */
    private fun back(state: MfaState): MfaUpdate = when (val step = state.step) {
        is MfaStep.WaitForPush -> when {
            step.isSending -> MfaUpdate(state)
            state.factors.size > 1 -> MfaUpdate(state.on(MfaStep.ChooseFactor), MfaCommand.StopPush)
            else -> MfaUpdate(state, MfaCommand.StopPush, MfaEffect.Restart)
        }

        MfaStep.ChooseFactor, is MfaStep.Failed -> MfaUpdate(state, effect = MfaEffect.Restart)

        MfaStep.Loading, MfaStep.Authenticated -> MfaUpdate(state)
    }

    private fun MfaState.on(step: MfaStep): MfaState = copy(step = step)
}
