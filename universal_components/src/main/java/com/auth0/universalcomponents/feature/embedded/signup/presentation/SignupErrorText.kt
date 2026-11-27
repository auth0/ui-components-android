package com.auth0.universalcomponents.feature.embedded.signup.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind

// Our words for each error, which are iOS's: the server's own text is never shown in sign-up. The words that are the
// login's are the login's strings.

/**
 * What the user can fix where they are, drawn under the field or the choices. A few name what is being verified, so
 * they follow [kind]: the one the user is on.
 */
@Composable
internal fun SignupError.Fixable.text(kind: SignupKind): String = stringResource(
    when (this) {
        SignupError.InvalidEmail -> R.string.embedded_login_error_invalid_email
        SignupError.InvalidPhone -> R.string.embedded_login_error_invalid_phone
        SignupError.InvalidCode -> when (kind) {
            SignupKind.Email -> R.string.embedded_signup_error_invalid_code
            SignupKind.Phone -> R.string.embedded_signup_error_invalid_code_phone
        }

        SignupError.CodeNotSent -> when (kind) {
            SignupKind.Email -> R.string.embedded_login_error_code_not_sent
            SignupKind.Phone -> R.string.embedded_login_error_code_not_sent_phone
        }
    },
)

/**
 * Why a sign-up ended or stopped, on the full-screen error. What the user can fix is shown by [text] with a kind, where
 * the screen is, and never gets here; the generic words are only a safe fallback. A code that timed out says to start
 * again: the phone's code screen, where a new one can be asked for, has words of its own.
 */
@Composable
internal fun SignupError.text(): String = stringResource(
    when (this) {
        is SignupError.Fixable -> R.string.embedded_login_error_generic
        SignupError.TooManyAttempts -> R.string.embedded_login_error_too_many_attempts
        SignupError.TooManyWrongCodes -> R.string.embedded_signup_error_too_many_wrong_codes
        SignupError.CodeExpired -> R.string.embedded_signup_error_code_expired
        SignupError.TooManyCodesRequested -> R.string.embedded_signup_error_too_many_codes_requested
        SignupError.Denied -> R.string.embedded_signup_error_denied
        // The account may exist when the tenant asked for more, but the words are iOS's: it says the same.
        SignupError.Ended, SignupError.MoreStepsRequired -> R.string.embedded_signup_error_ended
        SignupError.ConnectionMissing -> R.string.embedded_login_error_connection_missing
        SignupError.Unavailable -> R.string.embedded_signup_unavailable_message
        SignupError.Network -> R.string.embedded_login_error_network
        SignupError.ServerError, is SignupError.Unknown -> R.string.embedded_login_error_generic
    },
)
