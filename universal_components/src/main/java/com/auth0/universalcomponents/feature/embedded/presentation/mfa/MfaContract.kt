package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor

/*
 * The MFA flow is a loop: an input goes into [MfaReducer], which returns an [MfaUpdate]; [MfaEngine] shows the new
 * step, runs the commands and hands the effect to whoever hosts MFA. Whatever a command returns comes back in as
 * the next input.
 *
 *   inputs:  MfaIntent (the user did something) · MfaOutcome (the server answered) · a second passed
 *   outputs: MfaStep (what to draw) · MfaCommand (work to run) · MfaEffect (MFA is over)
 */

/** The MFA screen on show, with everything it draws; the steps are mutually exclusive, one per screen. */
internal sealed interface MfaStep {

    /** Every second factor the server offered, one row each. Shown only when there is more than one. */
    data class AuthenticatorList(val factors: List<MfaFactor>) : MfaStep

    /** Waiting on the user's device. [canChooseAnother] is true when there are other factors to go back to. */
    data class PushWaiting(
        val factor: MfaFactor.Push,
        val phase: PushPhase,
        val canChooseAnother: Boolean,
    ) : MfaStep

    /** The flow stopped. Try again sends [retry] again; when null, the sign-in starts over. */
    data class Failed(val error: EmbeddedLoginError, val retry: MfaFactor? = null) : MfaStep
}

/** Where a push is: still going out, or out and being checked. Only one can be true at a time. */
internal sealed interface PushPhase {

    /** The screen shows at once, while the push is sent. */
    data object Sending : PushPhase

    /** Checked again every [everyMs]; Resend opens once [resendSecondsLeft] reaches zero. */
    data class Polling(val everyMs: Long, val resendSecondsLeft: Int) : PushPhase
}

/** What the user did on an MFA screen. */
internal sealed interface MfaIntent {

    data class SelectFactor(val factor: MfaFactor) : MfaIntent

    /** "Choose another device": back to the list. Only offered when there is one. */
    data object ChooseAnotherFactor : MfaIntent

    /** System back or the top-bar arrow. */
    data object Back : MfaIntent

    data object Retry : MfaIntent

    /** Sends the push to the same device again. */
    data object Resend : MfaIntent
}

/**
 * What the server answered to a command, in our terms rather than the SDK's; only [MfaEngine] creates these,
 * from what the [com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository] returns.
 */
internal sealed interface MfaOutcome {

    /** The push is out; check it after [pollInMs]. */
    data class PushSent(val pollInMs: Long) : MfaOutcome

    /** Not approved yet; check again after [pollInMs], or after the same wait when null. */
    data class PushPending(val pollInMs: Long?) : MfaOutcome

    data class SignedIn(val credentials: Credentials) : MfaOutcome

    data class Failed(val failure: LoginFailure) : MfaOutcome
}

/**
 * Work [MfaReducer] asks [MfaEngine] to run, because the reducer itself never waits or calls the server.
 * An intent is the input from the user; a command is the output for the engine.
 */
internal sealed interface MfaCommand {

    data class SendPush(val factor: MfaFactor.Push) : MfaCommand

    data class CheckPush(val afterMs: Long) : MfaCommand

    /** Stops checking the push. */
    data object StopPush : MfaCommand

    /** Waits one second, then counts the resend wait down by one. */
    data object CountDownResend : MfaCommand
}

/**
 * How MFA ended, sent once to whoever hosts it. MFA never navigates: the host decides what each one means
 * (embedded login signs in, or goes back to email entry).
 */
internal sealed interface MfaEffect {

    data class Authenticated(val credentials: Credentials) : MfaEffect

    /** The session can't go on; sign-in starts over from the first factor. */
    data object Restart : MfaEffect
}

/** What [MfaReducer] returns for one input: the step to show, the [commands] to run in order, and maybe an effect. */
internal data class MfaUpdate(
    val step: MfaStep,
    val commands: List<MfaCommand> = emptyList(),
    val effect: MfaEffect? = null,
)
