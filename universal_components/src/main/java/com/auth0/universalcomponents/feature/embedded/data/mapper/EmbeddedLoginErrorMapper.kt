package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.Auth0Exception
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthException
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
        EmbeddedAuthError.AuthorizationRejected -> EmbeddedLoginError.PushDenied
        EmbeddedAuthError.NoEligibleFactors -> EmbeddedLoginError.NoEligibleFactors
        else -> null
    }
    val fromServer = code !in SDK_ERROR_CODES && description.isNotBlank()
    val loginError = known ?: if (fromServer) {
        EmbeddedLoginError.Server(description)
    } else {
        EmbeddedLoginError.Unknown("$code: $description")
    }
    return LoginFailure(loginError, canRetryStep, cause = this)
}

/** HTTP 5xx: the server failed, not the request. */
private val SERVER_ERROR_STATUS = 500..599

/** Codes the SDK makes up when the server sent no usable error. */
private val SDK_ERROR_CODES = setOf(
    Auth0Exception.UNKNOWN_ERROR,
    Auth0Exception.NON_JSON_ERROR,
    Auth0Exception.EMPTY_BODY_ERROR,
)
