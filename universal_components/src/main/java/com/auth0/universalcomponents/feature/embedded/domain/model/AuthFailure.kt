package com.auth0.universalcomponents.feature.embedded.domain.model

import androidx.annotation.StringRes
import com.auth0.universalcomponents.R

/** Recoverable failures, shown under the field on the current screen. Anything else goes full-screen. */
internal enum class AuthFailure(@StringRes val messageRes: Int) {
    TOO_MANY_ATTEMPTS(R.string.embedded_login_error_too_many_attempts),
    INVALID_CODE(R.string.embedded_login_error_invalid_code),
    CODE_NOT_SENT(R.string.embedded_login_error_code_not_sent),
    CODE_EXPIRED(R.string.embedded_login_error_code_expired),
}

internal class AuthFailureException(val failure: AuthFailure) : Exception()

/** The text on the full-screen error: the server's own words, or one of ours. */
internal sealed interface FailureMessage {

    data class Server(val text: String) : FailureMessage

    data class Local(@StringRes val textRes: Int) : FailureMessage
}

/**
 * A step failed for good; [displayMessage] is null when there's nothing better than the generic text.
 * [canRetryStep] is true when the server still holds the session, so the same step can be tried again.
 */
internal class EmbeddedAuthRequestFailed(
    val displayMessage: FailureMessage?,
    val canRetryStep: Boolean = false,
) : Exception()
