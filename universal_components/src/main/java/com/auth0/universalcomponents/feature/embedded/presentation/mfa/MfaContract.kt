package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

/**
 * The whole MFA flow in one value. [factors] is what the server offered; [step] is the screen on show.
 * [isStarted] keeps a second Start, after a rotation say, from starting over.
 */
internal data class MfaState(
    val factors: List<MfaFactor>,
    val step: MfaStep = MfaStep.Loading,
    val isStarted: Boolean = false,
)

/** One screen of the MFA flow, with everything it shows. */
internal sealed interface MfaStep {

    /** Before the flow starts. */
    data object Loading : MfaStep

    data object ChooseFactor : MfaStep

    /**
     * Waiting on the user's device; checked again every [pollInMs]. [isSending] is true until the push is out,
     * so the screen shows at once instead of a loader first.
     */
    data class WaitForPush(
        val factor: MfaFactor.Push,
        val pollInMs: Long = 0,
        val isSending: Boolean = false,
    ) : MfaStep

    /** Try again sends [retry] again; when null, the sign-in starts over. */
    data class Failed(val error: EmbeddedLoginError, val retry: MfaFactor? = null) : MfaStep

    data object Authenticated : MfaStep
}

/** What the user did. */
internal sealed interface MfaIntent {

    data object Start : MfaIntent

    data class SelectFactor(val factor: MfaFactor) : MfaIntent

    data object Back : MfaIntent

    data object Retry : MfaIntent
}

/** What the server answered; only the ViewModel sends these. */
internal sealed interface MfaResult {

    data class PushSent(val pollInMs: Long) : MfaResult

    data class PushPending(val pollInMs: Long?) : MfaResult

    data class SignedIn(val credentials: Credentials) : MfaResult

    data class Failed(val failure: LoginFailure) : MfaResult
}

/** Work the reducer asks the ViewModel to run. */
internal sealed interface MfaCommand {

    data class SendPush(val factor: MfaFactor.Push) : MfaCommand

    data class CheckPush(val afterMs: Long) : MfaCommand

    /** Stops checking the push; a check already in flight finishes first. */
    data object StopPush : MfaCommand
}

/** One-off outcomes for whoever hosts the MFA flow. */
internal sealed interface MfaEffect {

    data class Authenticated(val credentials: Credentials) : MfaEffect

    /** The session can't go on; sign-in starts over from the first factor. */
    data object Restart : MfaEffect
}

/** What one intent or result leads to. */
internal data class MfaUpdate(
    val state: MfaState,
    val command: MfaCommand? = null,
    val effect: MfaEffect? = null,
)
