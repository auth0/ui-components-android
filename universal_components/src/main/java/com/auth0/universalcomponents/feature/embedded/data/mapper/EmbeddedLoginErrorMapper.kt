package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.universalcomponents.data.embeddedauth.StepAnswer
import com.auth0.universalcomponents.data.embeddedauth.serverDescription
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginFailure

/** Names an unexpected failure in our terms, keeping it as the cause. */
internal fun Exception.toLoginFailure(): LoginFailure =
    LoginFailure(EmbeddedLoginError.Unknown(message ?: javaClass.name), cause = this)

/**
 * Names the SDK error in our terms. The SDK's own text is debug output, so only [EmbeddedLoginError.Unknown]
 * keeps it, for the logs; the user sees only the server's.
 */
internal fun EmbeddedAuthException.toLoginFailure(): LoginFailure {
    // The SDK keeps the session only for these; any other error ends it.
    val canRetryStep = error == EmbeddedAuthError.Network || statusCode in SERVER_ERROR_STATUS
    val known = when (error) {
        EmbeddedAuthError.Network -> EmbeddedLoginError.Network
        EmbeddedAuthError.ChallengeExpired -> EmbeddedLoginError.CodeExpired
        EmbeddedAuthError.TooManyWrongOtpAttempts -> EmbeddedLoginError.TooManyWrongCodes
        EmbeddedAuthError.TooManyAttempts -> EmbeddedLoginError.TooManyAttempts
        EmbeddedAuthError.TooManyLogins -> EmbeddedLoginError.TooManyLogins
        // The SDK's own text for these is developer advice ("Call authorize() first"), never for the user.
        EmbeddedAuthError.NoActiveSession, EmbeddedAuthError.SessionExpired -> EmbeddedLoginError.SessionExpired
        else -> null
    }
    val loginError = known ?: serverDescription?.let(EmbeddedLoginError::Server)
        ?: EmbeddedLoginError.Unknown("$code: $description")
    return LoginFailure(loginError, canRetryStep, cause = this)
}

/** HTTP 5xx: the server failed, not the request. */
private val SERVER_ERROR_STATUS = 500..599

/** A failed step in the login flow's terms. */
internal val StepAnswer.Failed.failure: LoginFailure
    get() = (error as? EmbeddedAuthException)?.toLoginFailure() ?: error.toLoginFailure()
