package com.auth0.universalcomponents.feature.embedded.domain.model

import com.auth0.universalcomponents.domain.error.ResultError

/**
 * Why MFA ended without signing the user in: the same cases, under the same names, as on iOS. Each case keeps the
 * failure behind it, for its stack trace; the cases MFA decides on its own have none. It's a [Throwable], like
 * [com.auth0.universalcomponents.domain.error.Auth0Error], so it can be logged and, later, thrown.
 */
internal sealed class MfaError(override val cause: Throwable?) : Throwable(cause), ResultError {

    /** None of the factors the server offered, in any round, can be used here. */
    data class NoEligibleFactors(override val cause: Throwable? = null) : MfaError(cause)

    /** The user turned the request down on their device. */
    data class Rejected(override val cause: Throwable?) : MfaError(cause)

    /** The request expired before the user answered it. */
    data class TokenExpired(override val cause: Throwable?) : MfaError(cause)

    /** The sign-in session ended, or was already used. */
    data class TokenInvalid(override val cause: Throwable?) : MfaError(cause)

    /** The server stopped the sign-in after too many attempts. */
    data class TooManyAttempts(override val cause: Throwable?) : MfaError(cause)

    /** A request couldn't reach the server, even after the quiet retries. */
    data class Network(override val cause: Throwable?) : MfaError(cause)

    /** Anything else. [serverMessage] is the server's own explanation, when it gave one. */
    data class Unexpected(val serverMessage: String? = null, override val cause: Throwable? = null) : MfaError(cause)
}
