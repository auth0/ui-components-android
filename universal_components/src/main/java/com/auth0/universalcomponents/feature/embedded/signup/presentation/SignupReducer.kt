package com.auth0.universalcomponents.feature.embedded.signup.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupIdentifier
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupProgress

/**
 * Every sign-up rule in one place: the state on show plus an input gives the next state, maybe with work to run and an
 * effect for the host. Pure, so it needs no Android, coroutines or SDK to test.
 *
 * An answer that arrives when no screen is waiting for it (the sign-up was left, or started over) is dropped: it can't
 * fail or advance a step the user is no longer on.
 */
internal class SignupReducer {

    val initialState: SignupState = SignupState.Idle

    fun handleIntent(state: SignupState, intent: SignupIntent): SignupUpdate = when (intent) {
        SignupIntent.Start -> if (state == SignupState.Idle) open() else SignupUpdate(state)
        is SignupIntent.SubmitIdentifier -> submitIdentifier(state, intent.identifier)
        is SignupIntent.SubmitCode -> submitCode(state, intent.code)
        is SignupIntent.ChooseDelivery -> chooseDelivery(state, intent.delivery)
        SignupIntent.Resend -> resend(state)
        SignupIntent.Retry -> retry(state)
        SignupIntent.Back -> goBack(state)
        SignupIntent.Leave -> SignupUpdate(SignupState.Idle, listOf(SignupCommand.CancelWork))
    }

    fun handleOutcome(state: SignupState, outcome: SignupOutcome): SignupUpdate = when (outcome) {
        is SignupOutcome.Opened -> if (state == SignupState.Loading) proceed(outcome.next) else SignupUpdate(state)
        is SignupOutcome.OpenFailed ->
            if (state == SignupState.Loading) openFailed(outcome.error) else SignupUpdate(state)
        is SignupOutcome.Identified -> identified(state, outcome)
        is SignupOutcome.CodeSent -> codeSent(state, outcome)
        is SignupOutcome.Verified -> verified(state, outcome.progress)
        is SignupOutcome.AccountCreated -> accountCreated(state, outcome)
        is SignupOutcome.RequestFailed -> requestFailed(state, outcome.error)
        is SignupOutcome.CreateFailed -> createFailed(state, outcome.error)
    }

    /**
     * [seconds] passed on the resend wait. It runs on the code screen, and on the error screen shown over it, so the
     * time spent there counts and "Try again" doesn't bring back a wait that stood still. Anywhere else the count is
     * over, and nothing goes on.
     */
    fun handleSecondsPassed(state: SignupState, seconds: Int): SignupUpdate {
        val waiting = state.waitingCode ?: return SignupUpdate(state)
        val counted = waiting.copy(resendSecondsLeft = (waiting.resendSecondsLeft - seconds).coerceAtLeast(0))
        return SignupUpdate(
            if (state is SignupState.Failed) state.copy(retry = SignupRetry.ReturnTo(counted)) else counted,
            commands = if (counted.resendSecondsLeft > 0) listOf(SignupCommand.CountDownResend) else emptyList(),
        )
    }

    // region intents

    private fun open() = SignupUpdate(SignupState.Loading, listOf(SignupCommand.Open))

    private fun submitIdentifier(state: SignupState, identifier: SignupIdentifier): SignupUpdate {
        if (state !is SignupState.Entry || state.isSubmitting || state.kind != identifier.kind) {
            return SignupUpdate(state)
        }
        return SignupUpdate(
            state.copy(value = identifier.value, isSubmitting = true, error = null),
            listOf(SignupCommand.Identify(identifier)),
        )
    }

    private fun submitCode(state: SignupState, code: String): SignupUpdate {
        if (state !is SignupState.Code || !state.acceptsCode || code.isBlank()) return SignupUpdate(state)
        return SignupUpdate(
            state.copy(phase = CodePhase.Verifying, error = null),
            listOf(SignupCommand.Verify(code)),
        )
    }

    /** Only a way the number takes, and not while the code is on its way. */
    private fun chooseDelivery(state: SignupState, delivery: PhoneDeliveryMethod): SignupUpdate {
        if (state !is SignupState.Delivery || state.isSubmitting || delivery !in state.challenge.deliveries) {
            return SignupUpdate(state)
        }
        return SignupUpdate(
            state.copy(isSubmitting = true, error = null),
            listOf(SignupCommand.SendCode(state.identifier, state.challenge, delivery)),
        )
    }

    /**
     * Only once the wait is over, and not while something else is on its way. The code goes the way the last one went.
     * A code that timed out ended the server's sign-up with it, so a new one starts it over for the same identifier.
     */
    private fun resend(state: SignupState): SignupUpdate {
        if (state !is SignupState.Code || state.phase != CodePhase.Ready || state.resendSecondsLeft > 0) {
            return SignupUpdate(state)
        }
        val command = if (state.isExpired) {
            SignupCommand.ReplaceExpiredCode(state.identifier, state.delivery)
        } else {
            SignupCommand.SendCode(state.identifier, state.challenge, state.delivery)
        }
        return SignupUpdate(state.copy(phase = CodePhase.Resending, error = null), listOf(command))
    }

    /** Only from the error screen, and it does what that screen's retry says: nothing to return to opens again. */
    private fun retry(state: SignupState): SignupUpdate {
        if (state !is SignupState.Failed) return SignupUpdate(state)
        return when (val retry = state.retry) {
            is SignupRetry.ReturnTo -> SignupUpdate(retry.state, commands = countdownIfWaiting(retry.state))
            SignupRetry.CreateAccount -> SignupUpdate(SignupState.Creating, listOf(SignupCommand.CreateAccount))
            SignupRetry.Reopen, null -> open()
        }
    }

    /**
     * Ignored while something is on its way. The code screen, and the one that asks how to get the code, go back to the
     * entry, with what was typed and nothing sent; anywhere else leaves, because a verified identifier can't be
     * changed, so there is nothing to go back to.
     */
    private fun goBack(state: SignupState): SignupUpdate = when {
        state.isBusy -> SignupUpdate(state)
        state is SignupState.Code -> backToEntry(state.identifier)
        state is SignupState.Delivery -> backToEntry(state.identifier)
        else -> SignupUpdate(state, effect = SignupEffect.Exit)
    }

    private fun backToEntry(identifier: SignupIdentifier) =
        SignupUpdate(SignupState.Entry(identifier.kind, identifier.value))

    // endregion

    // region outcomes

    /** One rule for every menu: create the account once every required identifier is verified, else ask for the next. */
    private fun proceed(next: SignupNext, from: SignupState = SignupState.Creating): SignupUpdate = when (next) {
        is SignupNext.AskFor -> SignupUpdate(SignupState.Entry(next.kind))
        SignupNext.ReadyToCreate -> SignupUpdate(from, listOf(SignupCommand.CreateAccount))
        // The server asks for nothing this flow can give, and there is nothing to try again.
        SignupNext.NothingToDo -> SignupUpdate(SignupState.Failed(SignupError.Unknown(NOTHING_TO_DO)))
    }

    private fun openFailed(error: SignupError): SignupUpdate = SignupUpdate(
        when (error) {
            SignupError.Unavailable -> SignupState.Unavailable
            is SignupError.Transient -> SignupState.Failed(error, SignupRetry.Reopen)
            else -> SignupState.Failed(error)
        },
    )

    /**
     * The identifier is accepted. A number that takes more than one way to get a code asks first how the user wants it;
     * otherwise the code goes out at once, with the spinner kept on Continue.
     */
    private fun identified(state: SignupState, outcome: SignupOutcome.Identified): SignupUpdate {
        if (state !is SignupState.Entry || !state.isSubmitting || state.kind != outcome.identifier.kind) {
            return SignupUpdate(state)
        }
        val challenge = outcome.challenge
        if (challenge.deliveries.size > 1) return SignupUpdate(SignupState.Delivery(outcome.identifier, challenge))
        return SignupUpdate(
            state,
            listOf(SignupCommand.SendCode(outcome.identifier, challenge, challenge.firstDelivery)),
        )
    }

    private fun codeSent(state: SignupState, outcome: SignupOutcome.CodeSent): SignupUpdate = when {
        // The first code: on to the code screen, with the wait before another starting.
        state is SignupState.Entry && state.isSubmitting && state.kind == outcome.identifier.kind ->
            codeScreen(SignupState.Code(outcome.identifier, outcome.challenge, outcome.delivery))

        // The first code, sent the way the user chose.
        state is SignupState.Delivery && state.isSubmitting && state.identifier == outcome.identifier ->
            codeScreen(SignupState.Code(outcome.identifier, outcome.challenge, outcome.delivery))

        // A resend: the same screen, with its error gone and the wait starting again. After a timeout the challenge is a
        // new one, and the code is no longer expired.
        state is SignupState.Code && state.phase == CodePhase.Resending && state.identifier == outcome.identifier ->
            codeScreen(
                state.copy(
                    challenge = outcome.challenge,
                    phase = CodePhase.Ready,
                    error = null,
                    resendSecondsLeft = RESEND_WAIT_SECONDS,
                    isExpired = false,
                ),
            )

        else -> SignupUpdate(state)
    }

    private fun codeScreen(state: SignupState.Code) = SignupUpdate(state, listOf(SignupCommand.CountDownResend))

    private fun verified(state: SignupState, progress: SignupProgress): SignupUpdate {
        if (state !is SignupState.Code || state.phase != CodePhase.Verifying) return SignupUpdate(state)
        return when (progress) {
            // An account already existed for the identifier, so the server signed that user in.
            is SignupProgress.SignedIn -> authenticated(progress.credentials)
            is SignupProgress.Continue -> when (val next = progress.next) {
                // Only when it is the same kind again is what was typed kept.
                is SignupNext.AskFor -> {
                    val typed = state.identifier.value.takeIf { state.identifier.kind == next.kind }.orEmpty()
                    SignupUpdate(SignupState.Entry(next.kind, value = typed))
                }
                // No extra screen: the spinner stays on Verify while the account is created.
                SignupNext.ReadyToCreate, SignupNext.NothingToDo -> proceed(next, from = state)
            }
        }
    }

    private fun accountCreated(state: SignupState, outcome: SignupOutcome.AccountCreated): SignupUpdate {
        val creating = state == SignupState.Creating || (state is SignupState.Code && state.phase == CodePhase.Verifying)
        return if (creating) authenticated(outcome.credentials) else SignupUpdate(state)
    }

    private fun authenticated(credentials: Credentials) =
        SignupUpdate(SignupState.Authenticated, effect = SignupEffect.Authenticated(credentials))

    /**
     * A request from the entry, delivery or code screen failed. What the user can fix stays on that screen with the
     * message under the field; what ends the sign-up goes full screen with "Start over"; anything else goes full screen
     * with "Try again", which returns to the screen as it was. The exception is a phone's code that timed out: the code
     * screen stays, and only a new code helps.
     */
    private fun requestFailed(state: SignupState, error: SignupError): SignupUpdate = when {
        state is SignupState.Entry && state.isSubmitting -> {
            val settled = state.copy(isSubmitting = false, error = null)
            failOn(error, settled) { state.copy(isSubmitting = false, error = it) }
        }

        state is SignupState.Delivery && state.isSubmitting -> {
            val settled = state.copy(isSubmitting = false, error = null)
            failOn(error, settled) { state.copy(isSubmitting = false, error = it) }
        }

        state is SignupState.Code && state.phase != CodePhase.Ready -> {
            val settled = state.copy(phase = CodePhase.Ready, error = null)
            if (error == SignupError.CodeExpired && state.identifier.kind.recoversExpiredCode) {
                SignupUpdate(settled.copy(isExpired = true, resendSecondsLeft = 0))
            } else {
                failOn(error, settled) { state.copy(phase = CodePhase.Ready, error = it) }
            }
        }

        else -> SignupUpdate(state)
    }

    private inline fun failOn(
        error: SignupError,
        settled: SignupState,
        stay: (SignupError.Fixable) -> SignupState,
    ): SignupUpdate = SignupUpdate(
        when (error) {
            is SignupError.Fixable -> stay(error)
            is SignupError.Transient -> SignupState.Failed(error, SignupRetry.ReturnTo(settled))
            else -> SignupState.Failed(error)
        },
    )

    /**
     * Creating the account failed. A lost connection keeps the sign-up where it is; the code isn't asked for again. That
     * holds while the SDK still has its session, which it doesn't once the server has made the account and only the
     * final token request failed: the retry then finds none and ends on "Start over", where signing up again with the
     * same email signs the user in.
     */
    private fun createFailed(state: SignupState, error: SignupError): SignupUpdate {
        val creating = state == SignupState.Creating || (state is SignupState.Code && state.phase == CodePhase.Verifying)
        if (!creating) return SignupUpdate(state)
        val failed = if (error is SignupError.Transient) {
            SignupState.Failed(error, SignupRetry.CreateAccount)
        } else {
            SignupState.Failed(error)
        }
        return SignupUpdate(failed)
    }

    // endregion

    /** A code may be checked: nothing is on its way, and this one hasn't timed out (only a new one helps then). */
    private val SignupState.Code.acceptsCode: Boolean
        get() = phase == CodePhase.Ready && !isExpired

    /** The code screen whose resend wait is running: the one on show, or the one an error screen will return to. */
    private val SignupState.waitingCode: SignupState.Code?
        get() = when (this) {
            is SignupState.Code -> this
            is SignupState.Failed -> (retry as? SignupRetry.ReturnTo)?.state as? SignupState.Code
            else -> null
        }

    /** A returned code screen goes on counting its wait down. */
    private fun countdownIfWaiting(state: SignupState): List<SignupCommand> =
        if (state is SignupState.Code && state.resendSecondsLeft > 0) {
            listOf(SignupCommand.CountDownResend)
        } else {
            emptyList()
        }

    private companion object {
        const val NOTHING_TO_DO = "The server asked for nothing this flow can give"
    }
}
