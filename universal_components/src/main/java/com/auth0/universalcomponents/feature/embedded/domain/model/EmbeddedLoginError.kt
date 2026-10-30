package com.auth0.universalcomponents.feature.embedded.domain.model

import com.auth0.universalcomponents.domain.error.ResultError

/** What went wrong in the login flow. It only names the problem; the presentation layer picks the words. */
internal sealed interface EmbeddedLoginError {

    data object ConnectionMissing : EmbeddedLoginError

    data object Network : EmbeddedLoginError

    /** The server offered no way to email a code, for example because only another method fits that user. */
    data object CodeNotSent : EmbeddedLoginError

    data object InvalidCode : EmbeddedLoginError

    data object CodeExpired : EmbeddedLoginError

    data object TooManyWrongCodes : EmbeddedLoginError

    data object TooManyAttempts : EmbeddedLoginError

    data object TooManyLogins : EmbeddedLoginError

    /**
     * The code was right, but the server wants a second factor first; [factors] are the ones it offered.
     * The flow hands over to MFA, so this one never reaches the error screen.
     */
    data class MfaRequired(val factors: List<MfaFactor>) : EmbeddedLoginError

    /** The sign-in session ended or expired, so it has to start over; the server or SDK no longer knows it. */
    data object SessionExpired : EmbeddedLoginError

    /** The user turned down the push on their device. */
    data object PushDenied : EmbeddedLoginError

    /** The account has no second factor it can use here. */
    data object NoEligibleFactors : EmbeddedLoginError

    /** The server asks for a further step that these screens can't show. */
    data object ExtraStepUnsupported : EmbeddedLoginError

    /** Any other error the server explained; [text] is its own words. */
    data class Server(val text: String) : EmbeddedLoginError

    /**
     * Nothing better to tell the user than the generic text.
     * [message] says what actually failed, for the logs; it's debug output, so it's never shown.
     */
    data class Unknown(val message: String) : EmbeddedLoginError
}

/**
 * How a login step failed; the repository returns it inside a [com.auth0.universalcomponents.domain.network.Result].
 * [canRetryStep] is true when the server still holds the session, so the same step can be tried again.
 * [cause] is the original failure, kept for its stack trace; it's null for the checks we make ourselves.
 */
internal data class LoginFailure(
    val error: EmbeddedLoginError,
    val canRetryStep: Boolean = false,
    val cause: Throwable? = null,
) : ResultError
