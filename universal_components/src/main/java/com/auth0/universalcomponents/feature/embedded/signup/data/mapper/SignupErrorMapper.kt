package com.auth0.universalcomponents.feature.embedded.signup.data.mapper

import com.auth0.android.embedded.EmbeddedAuthError
import com.auth0.android.embedded.EmbeddedAuthError.InsufficientAuthorization.Reason
import com.auth0.android.embedded.EmbeddedAuthException
import com.auth0.universalcomponents.data.embeddedauth.INVALID_CODE_REASONS
import com.auth0.universalcomponents.data.embeddedauth.StepAnswer
import com.auth0.universalcomponents.feature.embedded.data.mapper.SERVER_ERROR_STATUS
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupFailure

// The server's own words, which the SDK has no reason of its own for. `error_description` is the only thing that tells
// them apart, and it is read here and nowhere else. The server's text is never shown in sign-up.
private const val INVALID_EMAIL = "invalid_email"
private const val INVALID_PHONE = "invalid_phone"
private const val TOO_MANY_RESEND_ATTEMPTS = "too_many_resend_attempts"

/**
 * What a "not finished yet" answer says about what the user typed, or null when it is just the next menu. A wrong
 * email or phone number arrives as a plain continuation with no reason, so only its description tells it apart.
 */
internal fun StepAnswer.Next.toSignupFailure(): SignupFailure? = when {
    reason == Reason.NONE && description == INVALID_EMAIL -> SignupFailure(SignupError.InvalidEmail)
    reason == Reason.NONE && description == INVALID_PHONE -> SignupFailure(SignupError.InvalidPhone)
    reason in INVALID_CODE_REASONS -> SignupFailure(SignupError.InvalidCode)
    else -> null
}

/**
 * Names a failed step in our terms, keeping it as the cause. The SDK ends its session on every failure except a
 * network error or a server error (5xx), so anything else not named here ends the sign-up. The exception is the last
 * step that makes the account: the SDK has already cleared its session before it asks for the tokens, so a network or
 * server error there is retryable on paper, but the retry finds no session and ends the sign-up.
 */
internal fun StepAnswer.Failed.toSignupFailure(): SignupFailure {
    val exception = error as? EmbeddedAuthException
        ?: return SignupFailure(SignupError.Unknown(error.message ?: error.javaClass.name), cause = error)
    // The server failed, not the request, and the session is still there: the same step can be sent again.
    if (exception.statusCode in SERVER_ERROR_STATUS) return SignupFailure(SignupError.ServerError, cause = exception)
    val named = when (exception.error) {
        EmbeddedAuthError.TooManyAttempts, EmbeddedAuthError.TooManyLogins -> SignupError.TooManyAttempts
        EmbeddedAuthError.TooManyWrongOtpAttempts -> SignupError.TooManyWrongCodes
        EmbeddedAuthError.ChallengeExpired -> SignupError.CodeExpired
        EmbeddedAuthError.AccessDenied ->
            if (exception.description == TOO_MANY_RESEND_ATTEMPTS) SignupError.TooManyCodesRequested else SignupError.Denied
        EmbeddedAuthError.Network -> SignupError.Network
        else -> SignupError.Ended
    }
    return SignupFailure(named, cause = exception)
}
