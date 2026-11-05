package com.auth0.universalcomponents.feature.mfachallenge

import com.auth0.universalcomponents.domain.error.ResultError

/**
 * Why MFA ended without signing the user in: the same cases, under the same names, as iOS's `MfaError`, except
 * `credentialsNotSaved`, which is iOS's alone: this library never stores credentials, so there is nothing to fail.
 *
 * A later release may add cases, so give every `when` over this type an `else` branch. The compiler may call that
 * branch redundant today; it is what keeps your app working once a case is added.
 *
 * Each case keeps the failure behind it as its [cause], for an app that wants the SDK's own details (read it as
 * `error.cause as? EmbeddedAuthException`); the cases MFA decides on its own have none. [message] is null and
 * [toString] names the case only, so logging the error itself never writes a server message or a device name. The
 * [cause] can: it may hold the server's words, so don't show it to the user or send it to analytics as it is.
 */
sealed class MfaError(cause: Throwable?) : Exception(null as String?, cause), ResultError {

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

    // Written out rather than read from the class, whose name R8 shortens in a release build.
    override fun toString(): String = when (this) {
        is Cancelled -> "Cancelled"
        is TokenExpired -> "TokenExpired"
        is TokenInvalid -> "TokenInvalid"
        is Rejected -> "Rejected"
        is TooManyAttempts -> "TooManyAttempts"
        is NoEligibleFactors -> "NoEligibleFactors"
        is PresentationUnavailable -> "PresentationUnavailable"
        is Network -> "Network"
        is Unexpected -> "Unexpected"
    }
}

/**
 * What an app answers when it is told MFA ended with an error. It matters for one error only,
 * [MfaError.NoEligibleFactors]; for any other, the error screen already shows and the answer changes nothing.
 */
enum class MfaErrorHandling {

    /** The app takes over, for example by opening Universal Login. The user sees a spinner, not an error. */
    Handled,

    /** MFA shows its own "verification method not available" screen, with its way out. */
    NotHandled,
}
