package com.auth0.universalcomponents.feature.embedded.signup.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.presentation.CodeCooldown
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress

/*
 * A sign-up is a loop, as MFA is: an input goes into [SignupReducer], which returns a [SignupUpdate]; [SignupViewModel]
 * shows the new state, runs the commands and hands the effect to whoever hosts the sign-up. Whatever a command returns
 * comes back in as the next input.
 *
 *   inputs:  SignupIntent (the user did something) · SignupOutcome (the server answered) · a second has passed
 *   outputs: SignupState (what to draw) · SignupCommand (work to run) · SignupEffect (what the host must do)
 *
 * The server's menu decides each next screen, never a fixed order. A new kind of identifier is a new [SignupKind] and its
 * screens, and may need rules of its own, as a phone's choice of how to get the code and its expired code did: the
 * compiler lists the exhaustive `when`s on [SignupKind], but not the plain comparisons (`recoversExpiredCode`, a
 * delivery of `null` for an email) that must be checked by hand.
 */

/** The seconds a code has to wait before another may be asked for, as on the login's code screen. */
internal val RESEND_WAIT_SECONDS = CodeCooldown.DEFAULT_WAIT.inWholeSeconds.toInt()

/** The sign-up screen on show, with everything it draws; the states are mutually exclusive, one per screen. */
internal sealed interface SignupState {

    /** Nothing has run yet, or the sign-up was left. Drawn like [Loading], so the first frame is already the spinner. */
    data object Idle : SignupState

    /** The sign-up is opening. Back leaves, as nothing exists yet that leaving would lose. */
    data object Loading : SignupState

    /**
     * The account is being created with no code screen to hold the spinner: after "Try again", or when the server
     * needs nothing more to create it. Back waits, because leaving now would make the account and drop its credentials.
     */
    data object Creating : SignupState

    /**
     * Asks for one [kind]. [value] pre-fills the field with what was last submitted, so Edit, Back from the code screen
     * and Try again keep it; empty at first. [isSubmitting] spins Continue while the request is on its way, and [error]
     * is what the user can fix here, shown under the field.
     */
    data class Entry(
        val kind: SignupKind,
        val value: String = "",
        val isSubmitting: Boolean = false,
        val error: SignupError.Fixable? = null,
    ) : SignupState {

        /** [value] is what the user typed, personal data, so it is left out. */
        override fun toString(): String = "Entry(kind=$kind, isSubmitting=$isSubmitting, error=$error)"
    }

    /**
     * A phone [identifier] was accepted and its [challenge] lists more than one way to send the code, so the user
     * chooses before anything is sent. [isSubmitting] spins Continue while the code is on its way, and [error] is what
     * the user can fix here, shown under the choices.
     */
    data class Delivery(
        val identifier: SignupIdentifier,
        val challenge: SignupChallenge,
        val isSubmitting: Boolean = false,
        val error: SignupError.Fixable? = null,
    ) : SignupState

    /**
     * A [challenge] code went out to [identifier], the way [delivery] says (null for an email). [phase] says what is on
     * its way, and [error] is what the user can fix here, shown under the field. [resendSecondsLeft] counts down from
     * each code sent; Resend opens at zero. [isExpired] means the code timed out, which a phone's code screen survives:
     * only a new code helps, so Verify stays off and Resend is open at once.
     */
    data class Code(
        val identifier: SignupIdentifier,
        val challenge: SignupChallenge,
        val delivery: PhoneDeliveryMethod?,
        val phase: CodePhase = CodePhase.Ready,
        val error: SignupError.Fixable? = null,
        val resendSecondsLeft: Int = RESEND_WAIT_SECONDS,
        val isExpired: Boolean = false,
    ) : SignupState

    /** The account exists and the credentials went to the host. */
    data object Authenticated : SignupState

    /** The connection offers no sign-up this flow can finish. */
    data object Unavailable : SignupState

    /**
     * The sign-up couldn't go on, for [error], which is never one the user can fix on a field. [retry] says what
     * "Try again" does; null means the sign-up is over and only "Start over" follows.
     */
    data class Failed(val error: SignupError, val retry: SignupRetry? = null) : SignupState
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

/** Where "Try again" leads from [SignupState.Failed]. */
internal sealed interface SignupRetry {

    /** Back to the screen the request was made from, as it was. */
    data class ReturnTo(val state: SignupState) : SignupRetry

    /** The last code was verified and only the account is missing, so it is created again, without asking for the code. */
    data object CreateAccount : SignupRetry

    /** Nothing to go back to: the sign-up opens again. */
    data object Reopen : SignupRetry
}

/** What the user did on a sign-up screen. */
internal sealed interface SignupIntent {

    /** The screen appeared. Counts once: a repeat while the sign-up runs changes nothing. */
    data object Start : SignupIntent

    /** Continue on the entry screen; counts only on the entry for [identifier]'s kind. */
    data class SubmitIdentifier(val identifier: SignupIdentifier) : SignupIntent

    /** Sends the code the user typed. A live credential, so [toString] leaves it out. */
    data class SubmitCode(val code: String) : SignupIntent {
        override fun toString(): String = "SubmitCode"
    }

    /** Continue on the screen that asks how to get the code: it goes this way. Counts only if the number takes it. */
    data class ChooseDelivery(val delivery: PhoneDeliveryMethod) : SignupIntent

    /** A new code, to the same address as the last and the way the last went. */
    data object Resend : SignupIntent

    /** The button on the error screen, "Try again" or "Start over". */
    data object Retry : SignupIntent

    /** System back, the top-bar arrow, Edit on the code screen, or "Go back" when sign-up is unavailable. */
    data object Back : SignupIntent

    /** The host leaves sign-up: the next [Start] opens a new one. */
    data object Leave : SignupIntent
}

/**
 * What the server answered to a command, in sign-up's terms rather than the SDK's; only [SignupViewModel] creates
 * these, from what the repository returns.
 */
internal sealed interface SignupOutcome {

    data class Opened(val next: SignupNext) : SignupOutcome

    data class OpenFailed(val error: SignupError) : SignupOutcome

    /** The server accepted [identifier] and will send [challenge]. */
    data class Identified(val identifier: SignupIdentifier, val challenge: SignupChallenge) : SignupOutcome

    /** The code for [challenge] is out, to [identifier], the way [delivery] says. After a timeout it is a new challenge. */
    data class CodeSent(
        val identifier: SignupIdentifier,
        val challenge: SignupChallenge,
        val delivery: PhoneDeliveryMethod?,
    ) : SignupOutcome

    data class Verified(val progress: SignupProgress) : SignupOutcome

    /** The account was created. Holds the credentials, so [toString] leaves them out. */
    data class AccountCreated(val credentials: Credentials) : SignupOutcome {
        override fun toString(): String = "AccountCreated"
    }

    /** A request from the entry, delivery or code screen failed: an identifier sent, a code sent, or a code checked. */
    data class RequestFailed(val error: SignupError) : SignupOutcome

    data class CreateFailed(val error: SignupError) : SignupOutcome
}

/**
 * Work [SignupReducer] asks [SignupViewModel] to run, because the reducer itself never waits or calls the server. An
 * intent is the input from the user; a command is the output for the view model.
 */
internal sealed interface SignupCommand {

    /** Opens a sign-up, which also starts it over. */
    data object Open : SignupCommand

    data class Identify(val identifier: SignupIdentifier) : SignupCommand

    /**
     * Sends the code for [challenge], to [identifier], the way [delivery] says (null for an email). A resend is the
     * same command.
     */
    data class SendCode(
        val identifier: SignupIdentifier,
        val challenge: SignupChallenge,
        val delivery: PhoneDeliveryMethod?,
    ) : SignupCommand

    /** A new sign-up for [identifier], whose last code timed out, and a code for it the way [delivery] says. */
    data class ReplaceExpiredCode(val identifier: SignupIdentifier, val delivery: PhoneDeliveryMethod?) : SignupCommand

    /** Checks the [code] the user typed. A live credential, so [toString] leaves it out. */
    data class Verify(val code: String) : SignupCommand {
        override fun toString(): String = "Verify"
    }

    data object CreateAccount : SignupCommand

    /** Waits one second, then counts the resend wait on show down by that many; the reducer decides whether to go on. */
    data object CountDownResend : SignupCommand

    /** Stops what is on its way: the sign-up was left, so nothing it started may reach the screen. */
    data object CancelWork : SignupCommand
}

/** What the host must do, once; sign-up never navigates. */
internal sealed interface SignupEffect {

    /** The account exists, or the identifier already had one: the credentials go to the app. */
    data class Authenticated(val credentials: Credentials) : SignupEffect {
        override fun toString(): String = "Authenticated"
    }

    /** The user leaves sign-up: back to the login when it was opened from there, else out of the flow. */
    data object Exit : SignupEffect
}

/** What [SignupReducer] returns for one input: the state to show, the [commands] to run in order, and maybe an effect. */
internal data class SignupUpdate(
    val state: SignupState,
    val commands: List<SignupCommand> = emptyList(),
    val effect: SignupEffect? = null,
)

/** A request is on its way, so Back waits and the screen fades what can't be tapped. */
internal val SignupState.isBusy: Boolean
    get() = when (this) {
        is SignupState.Entry -> isSubmitting
        is SignupState.Delivery -> isSubmitting
        is SignupState.Code -> phase != CodePhase.Ready
        SignupState.Creating -> true
        else -> false
    }
