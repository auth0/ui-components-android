package com.auth0.universalcomponents.feature.embedded.signup.domain.model

import com.auth0.universalcomponents.domain.error.ResultError

/**
 * Why a sign-up step didn't go through. It only names the problem; the presentation layer picks the words. The groups
 * say what the user can do about it: fix it where they are, start over, or try the same step again.
 */
internal sealed interface SignupError {

    /** The user can fix it on the screen they are on: the sign-up goes on, with a message under the field. */
    sealed interface Fixable : SignupError

    /**
     * The sign-up is over: only "Start over" follows, because the SDK clears its session on every failure except a
     * continuation, a network error or a server error (5xx), so another try would find none.
     */
    sealed interface Fatal : SignupError

    /** The request never arrived, or failed in a way that left the session: the same step can be tried again. */
    sealed interface Transient : SignupError

    data object InvalidEmail : Fixable

    data object InvalidCode : Fixable

    /** The server offered no way to send a code. */
    data object CodeNotSent : Fixable

    data object TooManyAttempts : Fatal

    data object TooManyWrongCodes : Fatal

    data object CodeExpired : Fatal

    data object TooManyCodesRequested : Fatal

    /** Refused before the account existed, for example by a pre-registration Action. */
    data object Denied : Fatal

    /** Anything else that closed the session, or an answer this flow has no screen for. */
    data object Ended : Fatal

    /** The account was created, but the tenant wants more (a second factor, say), which this flow doesn't run yet. */
    data object MoreStepsRequired : Fatal

    /** No connection was given, so opening again can't change that. */
    data object ConnectionMissing : Fatal

    /** The connection offers no sign-up this flow can finish. Only opening a sign-up returns it. */
    data object Unavailable : Fatal

    data object Network : Transient

    /** Nothing better to say. [message] says what actually failed, for the logs; it's debug output, never shown. */
    data class Unknown(val message: String) : Transient
}

/**
 * How a sign-up step failed; the repository returns it inside a [com.auth0.universalcomponents.domain.network.Result].
 * [cause] is the original failure, kept for its stack trace; it's null for the checks we make ourselves.
 */
internal data class SignupFailure(
    val error: SignupError,
    val cause: Throwable? = null,
) : ResultError
