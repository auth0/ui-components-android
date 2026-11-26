package com.auth0.universalcomponents.feature.embedded.signup.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupError
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind

/**
 * Our words for each error, which are iOS's: the server's own text is never shown in sign-up. The words that are the
 * login's are the login's strings. A few name what is being verified, so they follow [kind]: the one the user is on.
 */
@Composable
internal fun SignupError.text(kind: SignupKind): String = stringResource(
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

        SignupError.TooManyAttempts -> R.string.embedded_login_error_too_many_attempts
        SignupError.TooManyWrongCodes -> R.string.embedded_signup_error_too_many_wrong_codes
        // A kind that recovers asks for a new code; one that doesn't ends the sign-up, so its words say to start again.
        SignupError.CodeExpired -> if (kind.recoversExpiredCode) {
            R.string.embedded_signup_error_code_expired_phone
        } else {
            R.string.embedded_signup_error_code_expired
        }

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
