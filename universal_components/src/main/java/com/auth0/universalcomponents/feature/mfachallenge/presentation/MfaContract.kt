package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.PushStatus

/*
 * The MFA flow is a loop: an input goes into [MfaReducer], which returns an [MfaUpdate]; [MfaEngine] shows the new
 * step, runs the commands and hands the effect to whoever hosts MFA. Whatever a command returns comes back in as
 * the next input.
 *
 *   inputs:  MfaIntent (the user did something) · MfaOutcome (the server answered) · a wait is over
 *   outputs: MfaState (what to draw) · MfaCommand (work to run) · MfaEffect (MFA is over)
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
internal sealed interface MfaState {

    /** The round's factors, one row each. Shown only when there is more than one. */
    data class ChooseFactor(val round: MfaRound) : MfaState

    /**
     * [factor]'s challenge is going out, and its screen shows once it's out. With a choice the list stays, with that
     * row busy; with one factor, a loading screen shows.
     */
    data class SendingChallenge(val round: MfaRound, val factor: MfaFactor) : MfaState

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
    ) : MfaState

    /**
     * A code went to the phone or the email address of [factor] and waits to be typed. [phase] says what is on its way;
     * [isWrongCode] is true after the server refused the last code. [canResend] is the server's say; Resend opens once
     * [resendSecondsLeft] also reaches zero.
     */
    data class CodeEntry(
        val round: MfaRound,
        val factor: MfaFactor.Code,
        val phase: CodePhase,
        val canResend: Boolean,
        val resendSecondsLeft: Int,
        val isWrongCode: Boolean = false,
    ) : MfaState

    /** MFA ended without signing in, and this screen says why. Leaving it is up to the host. */
    data class Failed(val error: MfaError) : MfaState
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

/** What is on its way for the code screen on show; only one at a time. */
internal enum class CodePhase {

    /** Nothing: the user may type a code. */
    Ready,

    /** The typed code is being checked. */
    Verifying,

    /** The code is being sent again. */
    Resending,
}

/** What the user did on an MFA screen. */
internal sealed interface MfaIntent {

    data class SelectFactor(val factor: MfaFactor) : MfaIntent

    /** "Verify another way": back to the list. Only offered when there is a choice. */
    data object ChooseAnotherFactor : MfaIntent

    /** System back or the top-bar arrow. */
    data object Back : MfaIntent

    /** The app's Cancel on any screen: leaves MFA at once, where Back may only step back to the list. */
    data object Cancel : MfaIntent

    /** Sends the code the user typed. */
    data class SubmitCode(val code: String) : MfaIntent

    /** Sends the push, or the code, to the same device, phone or address again. */
    data object Resend : MfaIntent
}

/**
 * What the server answered to a command, in MFA's terms rather than the SDK's; only [MfaEngine] creates these,
 * from what the [com.auth0.universalcomponents.feature.mfachallenge.domain.repository.MfaRepository] returns.
 */
internal sealed interface MfaOutcome {

    /** The push is out; [pending] says when to check it and whether it may be sent again. */
    data class PushSent(val pending: PushStatus.Pending) : MfaOutcome

    /** Not approved yet; [pending] says when to check again. */
    data class PushPending(val pending: PushStatus.Pending) : MfaOutcome

    data class SignedIn(val credentials: Credentials) : MfaOutcome

    /** The user denied the push; [cause] is the SDK reply behind it. */
    data class PushRejected(val cause: Throwable?) : MfaOutcome

    /** Approved, and the server asks for one more factor: [factors] are the next round's. */
    data class AnotherFactor(val factors: List<MfaFactor>) : MfaOutcome

    /** The code is out; [canResend] says whether the server still offers to send it again. */
    data class CodeSent(val canResend: Boolean) : MfaOutcome

    /** The server did not accept the typed code. */
    data object CodeRejected : MfaOutcome

    /** Checking the typed code failed for a reason other than a wrong code. */
    data class CodeFailed(val error: MfaError) : MfaOutcome

    /** The push or the code couldn't be sent. */
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

    data class SendPhoneCode(val factor: MfaFactor.Phone) : MfaCommand

    data class SendEmailCode(val factor: MfaFactor.Email) : MfaCommand

    data class VerifyCode(val code: String) : MfaCommand

    /** Waits [afterMs], then says the next check is due; the reducer decides whether it still is. */
    data class SchedulePushCheck(val afterMs: Long) : MfaCommand

    /** Checks the push once, now. */
    data object CheckPush : MfaCommand

    /**
     * Cancels the wait before the next check, and a push or code still waiting for its turn at the SDK, so one
     * the user has left behind is never sent. A call already on its way still finishes: the SDK can't stop it, and its
     * answer is dropped.
     */
    data object CancelChallengeWork : MfaCommand

    /** Waits one second, then counts the resend wait of the push or code on show down by one. */
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

    /** MFA failed without signing in; its [MfaState.Failed] screen stays until the user leaves it. */
    data class Failed(val error: MfaError) : MfaEffect
}

/** What [MfaReducer] returns for one input: the state to show, the [commands] to run in order, and maybe an effect. */
internal data class MfaUpdate(
    val state: MfaState,
    val commands: List<MfaCommand> = emptyList(),
    val effect: MfaEffect? = null,
)
