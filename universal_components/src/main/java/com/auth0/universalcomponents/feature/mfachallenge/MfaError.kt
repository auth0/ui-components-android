package com.auth0.universalcomponents.feature.mfachallenge

import com.auth0.universalcomponents.domain.error.ResultError

/**
 * Why MFA ended without signing the user in: the same cases, under the same names, as on iOS. A later release may add
 * cases, so give every `when` over this type an `else` branch.
 *
 * Each case keeps the failure behind it as its [cause], for an app that wants the SDK's own details (read it as
 * `error.cause as? EmbeddedAuthException`); the cases MFA decides on its own have none. [toString] names the case only,
 * so logging an error never writes a server message or a device name.
 */
sealed class MfaError(cause: Throwable?) : Exception(cause), ResultError {

    /** The user left MFA. Only a headless app sees it, as `MfaResult.Ended`; it is never sent to `onMfaError`. */
    class Cancelled : MfaError(null)

    /** The request expired before the user answered it. */
    class TokenExpired(cause: Throwable? = null) : MfaError(cause)

    /** The sign-in session ended, or was already used. */
    class TokenInvalid(cause: Throwable? = null) : MfaError(cause)

    /** The user turned the request down on their device. */
    class Rejected(cause: Throwable? = null) : MfaError(cause)

    /** The server stopped the sign-in after too many attempts. */
    class TooManyAttempts(cause: Throwable? = null) : MfaError(cause)

    /** None of the factors the server offered, in any round, can be used here. */
    class NoEligibleFactors(cause: Throwable? = null) : MfaError(cause)

    /** Reserved for step-up, which needs a screen to show MFA on. Embedded login never ends with it. */
    class PresentationUnavailable : MfaError(null)

    /** A request couldn't reach the server, even after the quiet retries. */
    class Network(cause: Throwable? = null) : MfaError(cause)

    /** Anything else. [serverMessage] is the server's own explanation, when it gave one. */
    class Unexpected(val serverMessage: String? = null, cause: Throwable? = null) : MfaError(cause)

    override fun toString(): String = this::class.java.simpleName
}
