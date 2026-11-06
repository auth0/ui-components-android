package com.auth0.universalcomponents.feature.embedded.domain.model

import com.auth0.universalcomponents.domain.error.ResultError
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor

/** What went wrong in the login flow. It only names the problem; the presentation layer picks the words. */
internal sealed interface EmbeddedLoginError {

    data object ConnectionMissing : EmbeddedLoginError

    data object Network : EmbeddedLoginError

    /** The server offered no way to send a code by [method], for example because only another method fits that user. */
    data class CodeNotSent(val method: LoginOption) : EmbeddedLoginError

    /** The server refused the email address or phone number as typed (for example a number that is too short). */
    data class InvalidIdentifier(val method: LoginOption) : EmbeddedLoginError

    data object InvalidCode : EmbeddedLoginError

    data object CodeExpired : EmbeddedLoginError

    data object TooManyWrongCodes : EmbeddedLoginError

    data object TooManyAttempts : EmbeddedLoginError

    data object TooManyLogins : EmbeddedLoginError

    /**
     * The code was right, but the server wants a second factor first; [factors] are the ones it offered that these
     * screens support, maybe none. The flow hands over to MFA, which decides what to show, so this one never reaches
     * the error screen.
     */
    data class MfaRequired(val factors: List<MfaFactor>) : EmbeddedLoginError {

        companion object {
            /**
             * A second factor is required, but none can be used here: the server said the account has no eligible
             * factor, or offered only ones these screens don't support. MFA ends on its "not available" screen.
             */
            val NoUsableFactor = MfaRequired(emptyList())
        }
    }

    /** The sign-in session ended or expired, so it has to start over; the server or SDK no longer knows it. */
    data object SessionExpired : EmbeddedLoginError

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
