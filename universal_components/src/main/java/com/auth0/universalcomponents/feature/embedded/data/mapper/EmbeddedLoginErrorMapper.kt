package com.auth0.universalcomponents.feature.embedded.data.mapper

import com.auth0.android.Auth0Exception
import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs one login call so that only an [EmbeddedLoginException] leaves it, with the original failure as its cause.
 * Rethrows [CancellationException] first, to preserve structured concurrency.
 */
// Intentionally catches all exceptions to name them in our terms; CancellationException is rethrown first.
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> withLoginErrorMapping(execute: suspend () -> T): T {
    return try {
        execute()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.asLoginException()
    }
}

/** Names any failure in our terms; one already named passes through as it is. */
private fun Exception.asLoginException(): EmbeddedLoginException = when (this) {
    is EmbeddedLoginException -> this
    is EmbeddedAuthException -> toLoginException()
    else -> EmbeddedLoginException(EmbeddedLoginError.Unknown, cause = this)
}

/** A wrong code also answers "not finished yet"; only its reason tells it apart from a further step. */
internal fun EmbeddedAuthException.toVerifyFailure(): EmbeddedLoginException {
    val sdkError = error as? EmbeddedAuthError.InsufficientAuthorization ?: return toLoginException()
    val loginError = if (sdkError.reason in INVALID_CODE_REASONS) {
        EmbeddedLoginError.InvalidCode
    } else {
        EmbeddedLoginError.ExtraStepUnsupported
    }
    return EmbeddedLoginException(loginError, cause = this)
}

/** Names the SDK error in our terms. Never keeps the SDK's own text, which is debug output; only the server's. */
internal fun EmbeddedAuthException.toLoginException(): EmbeddedLoginException {
    // The SDK keeps the session only for these; any other error ends it.
    val canRetryStep = error == EmbeddedAuthError.Network || statusCode in SERVER_ERROR_STATUS
    val known = when (error) {
        EmbeddedAuthError.Network -> EmbeddedLoginError.Network
        EmbeddedAuthError.ChallengeExpired -> EmbeddedLoginError.CodeExpired
        EmbeddedAuthError.TooManyWrongOtpAttempts -> EmbeddedLoginError.TooManyWrongCodes
        EmbeddedAuthError.TooManyAttempts -> EmbeddedLoginError.TooManyAttempts
        EmbeddedAuthError.TooManyLogins -> EmbeddedLoginError.TooManyLogins
        else -> null
    }
    val fromServer = code !in SDK_ERROR_CODES && description.isNotBlank()
    val loginError = known ?: if (fromServer) EmbeddedLoginError.Server(description) else EmbeddedLoginError.Unknown
    return EmbeddedLoginException(loginError, canRetryStep, cause = this)
}

private val INVALID_CODE_REASONS = setOf(Reason.INVALID_CODE, Reason.INVALID_IDENTIFIER_OR_CODE)

/** HTTP 5xx: the server failed, not the request. */
private val SERVER_ERROR_STATUS = 500..599

/** Codes the SDK makes up when the server sent no usable error. */
private val SDK_ERROR_CODES = setOf(
    Auth0Exception.UNKNOWN_ERROR,
    Auth0Exception.NON_JSON_ERROR,
    Auth0Exception.EMPTY_BODY_ERROR,
)
