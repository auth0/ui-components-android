package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaError
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.domain.model.PushStatus

/*
 * The MFA flow is a loop: an input goes into [MfaReducer], which returns an [MfaUpdate]; [MfaEngine] shows the new
 * step, runs the commands and hands the effect to whoever hosts MFA. Whatever a command returns comes back in as
 * the next input.
 *
 *   inputs:  MfaIntent (the user did something) · MfaOutcome (the server answered) · a wait is over
 *   outputs: MfaStep (what to draw) · MfaCommand (work to run) · MfaEffect (MFA is over)
 *
 * The rules are the ones iOS's MFA Core follows, so a sign-in behaves the same on both platforms. One difference, from
 * the PRD's additional challenge: in round 2 and later the list shows even with a single factor.
 */

/**
 * The factors one round offers. Round 1 follows the first factor; each time the server asks for one more factor, the
 * next round starts with its own menu. The server decides how many rounds a sign-in needs; there's no limit here.
 */
internal data class MfaRound(val number: Int, val factors: List<MfaFactor>) {

    /** There's a choice, so "Verify another way" is offered. */
    val canSwitch: Boolean
        get() = factors.size > 1

    /** From round 2 on, the server has asked for one more factor. */
    val isAdditional: Boolean
        get() = number > 1

    /** The list is on show: with a choice, and in every additional round, so the user sees why they're asked again. */
    val showsList: Boolean
        get() = canSwitch || isAdditional
}

/** The MFA screen on show, with everything it draws; the steps are mutually exclusive, one per screen. */
internal sealed interface MfaStep {

    /** The round's factors, one row each. Shown only when there is more than one. */
    data class ChooseFactor(val round: MfaRound) : MfaStep

    /**
     * [factor]'s challenge is going out, and its screen shows once it's out. With a choice the list stays, with that
     * row busy; with one factor, a loading screen shows.
     */
    data class SendingChallenge(val round: MfaRound, val factor: MfaFactor) : MfaStep

    /**
     * The push is out and waits for the user's approval; [phase] says what's on its way. It's checked every
     * [everyMs]. [canResend] is the server's say; Resend opens once [resendSecondsLeft] also reaches zero.
     * [failedChecks] counts the checks in a row that failed on the network and were retried quietly.
     */
    data class PushWaiting(
        val round: MfaRound,
        val factor: MfaFactor.Push,
        val phase: PushPhase,
        val everyMs: Long,
        val canResend: Boolean,
        val resendSecondsLeft: Int,
        val failedChecks: Int = 0,
    ) : MfaStep

    /** MFA ended without signing in, and this screen says why. Leaving it is up to the host. */
    data class Failed(val error: MfaError) : MfaStep
}

/** What's on its way for the push on show; only one at a time. */
internal sealed interface PushPhase {

    /** Waiting out the interval before the next check. */
    data object Waiting : PushPhase

    /** A check is on its way. */
    data object Checking : PushPhase

    /** The push is being sent again; there are no checks until it's out. */
    data object Resending : PushPhase
}

/** What the user did on an MFA screen. */
internal sealed interface MfaIntent {

    data class SelectFactor(val factor: MfaFactor) : MfaIntent

    /** "Verify another way": back to the list. Only offered when there is a choice. */
    data object ChooseAnotherFactor : MfaIntent

    /** System back or the top-bar arrow. */
    data object Back : MfaIntent

    /** Sends the push to the same device again. */
    data object Resend : MfaIntent
}

/**
 * What the server answered to a command, in MFA's terms rather than the SDK's; only [MfaEngine] creates these,
 * from what the [com.auth0.universalcomponents.feature.embedded.domain.repository.MfaRepository] returns.
 */
internal sealed interface MfaOutcome {

    /** The push is out; [pending] says when to check it and whether it may be sent again. */
    data class PushSent(val pending: PushStatus.Pending) : MfaOutcome

    /** Not approved yet; [pending] says when to check again. */
    data class PushPending(val pending: PushStatus.Pending) : MfaOutcome

    data class SignedIn(val credentials: Credentials) : MfaOutcome

    /** Approved, and the server asks for one more factor: [factors] are the next round's. */
    data class AnotherFactor(val factors: List<MfaFactor>) : MfaOutcome

    /** The push couldn't be sent. */
    data class SendFailed(val error: MfaError) : MfaOutcome

    /**
     * A check on a push already sent failed. [whileInBackground] is true when the app was in the background when it
     * answered, where Android may cut its network (Data Saver, for example) while the user approves elsewhere.
     */
    data class CheckFailed(val error: MfaError, val whileInBackground: Boolean = false) : MfaOutcome
}

/**
 * Work [MfaReducer] asks [MfaEngine] to run, because the reducer itself never waits or calls the server.
 * An intent is the input from the user; a command is the output for the engine.
 */
internal sealed interface MfaCommand {

    data class SendPush(val factor: MfaFactor.Push) : MfaCommand

    /** Waits [afterMs], then says the next check is due; the reducer decides whether it still is. */
    data class SchedulePushCheck(val afterMs: Long) : MfaCommand

    /** Checks the push once, now. */
    data object CheckPush : MfaCommand

    /**
     * Cancels the wait before the next check, and a push still waiting for its turn at the SDK, so a push the user has
     * left behind is never sent. A call already on its way still finishes: the SDK can't stop it, and its answer is
     * dropped.
     */
    data object CancelPushWork : MfaCommand

    /** Waits one second, then counts the resend wait down by one. */
    data object CountDownResend : MfaCommand
}

/**
 * How MFA ended, sent once to whoever hosts it. MFA never navigates: the host decides what each one means
 * (embedded login signs in, or goes back to email entry).
 */
internal sealed interface MfaEffect {

    data class Authenticated(val credentials: Credentials) : MfaEffect

    /** The user left MFA, so the sign-in starts over from the first factor. Not an error, so it isn't reported. */
    data object Restart : MfaEffect

    /** MFA failed without signing in; its [MfaStep.Failed] screen stays until the user leaves it. */
    data class Failed(val error: MfaError) : MfaEffect
}

/** What [MfaReducer] returns for one input: the step to show, the [commands] to run in order, and maybe an effect. */
internal data class MfaUpdate(
    val step: MfaStep,
    val commands: List<MfaCommand> = emptyList(),
    val effect: MfaEffect? = null,
)
