package com.auth0.universalcomponents.feature.mfachallenge.presentation

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaMessages
import com.auth0.universalcomponents.feature.mfachallenge.ResolvedMfaMessages

/** The words in the language the screen is in now: read again when the language changes. */
@Composable
internal fun MfaMessages.resolve(): ResolvedMfaMessages {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(this, context, configuration) { resolve(context.resources) }
}

/**
 * The app's words where it set some, the string resources' where not. A word with no argument is read with
 * `getString(id)`: `getString(id, args)` always formats, so an app's word with a `%` in it (`100% sure`) would throw.
 */
internal fun MfaMessages.resolve(resources: Resources): ResolvedMfaMessages {
    fun word(override: String?, @StringRes id: Int) = override ?: resources.getString(id)

    fun message(override: MfaMessages.Message, @StringRes title: Int, @StringRes description: Int) =
        ResolvedMfaMessages.Message(word(override.title, title), word(override.description, description))

    val headers = authenticatorList.headers
    val push = pushChallenge
    return ResolvedMfaMessages(
        authenticatorList = ResolvedMfaMessages.AuthenticatorList(
            ResolvedMfaMessages.AuthenticatorList.Headers(
                challenge = message(
                    headers.challenge,
                    R.string.auth0_mfa_authenticator_list_headers_challenge_title,
                    R.string.auth0_mfa_authenticator_list_headers_challenge_description,
                ),
                challengeAdditional = message(
                    headers.challengeAdditional,
                    R.string.auth0_mfa_authenticator_list_headers_challenge_additional_title,
                    R.string.auth0_mfa_authenticator_list_headers_challenge_additional_description,
                ),
            ),
        ),
        loading = ResolvedMfaMessages.Loading(
            word(loading.accessibilityLabel, R.string.auth0_mfa_loading_accessibility_label),
        ),
        errors = ResolvedMfaMessages.Errors(
            startOver = word(errors.startOver, R.string.auth0_mfa_errors_start_over),
            close = word(errors.close, R.string.auth0_mfa_errors_close),
            messageByError = mapOf(
                MfaError.Cancelled::class.java to message(
                    errors.cancelled,
                    R.string.auth0_mfa_errors_cancelled_title,
                    R.string.auth0_mfa_errors_cancelled_description,
                ),
                MfaError.TokenExpired::class.java to message(
                    errors.tokenExpired,
                    R.string.auth0_mfa_errors_token_expired_title,
                    R.string.auth0_mfa_errors_token_expired_description,
                ),
                MfaError.TokenInvalid::class.java to message(
                    errors.tokenInvalid,
                    R.string.auth0_mfa_errors_token_invalid_title,
                    R.string.auth0_mfa_errors_token_invalid_description,
                ),
                MfaError.Rejected::class.java to message(
                    errors.rejected,
                    R.string.auth0_mfa_errors_rejected_title,
                    R.string.auth0_mfa_errors_rejected_description,
                ),
                MfaError.TooManyAttempts::class.java to message(
                    errors.tooManyAttempts,
                    R.string.auth0_mfa_errors_too_many_attempts_title,
                    R.string.auth0_mfa_errors_too_many_attempts_description,
                ),
                MfaError.NoEligibleFactors::class.java to message(
                    errors.noEligibleFactors,
                    R.string.auth0_mfa_errors_no_eligible_factors_title,
                    R.string.auth0_mfa_errors_no_eligible_factors_description,
                ),
                MfaError.Network::class.java to message(
                    errors.network,
                    R.string.auth0_mfa_errors_network_title,
                    R.string.auth0_mfa_errors_network_description,
                ),
                MfaError.Unexpected::class.java to message(
                    errors.unexpected,
                    R.string.auth0_mfa_errors_unexpected_title,
                    R.string.auth0_mfa_errors_unexpected_description,
                ),
            ),
        ),
        pushChallenge = ResolvedMfaMessages.PushChallenge(
            row = message(
                push.row,
                R.string.auth0_mfa_push_challenge_row_title,
                R.string.auth0_mfa_push_challenge_row_description,
            ),
            title = word(push.title, R.string.auth0_mfa_push_challenge_title),
            description = push.description ?: { device ->
                resources.getString(R.string.auth0_mfa_push_challenge_description, device)
            },
            descriptionUnnamed = word(push.descriptionUnnamed, R.string.auth0_mfa_push_challenge_description_unnamed),
            waiting = word(push.waiting, R.string.auth0_mfa_push_challenge_waiting),
            resend = ResolvedMfaMessages.PushChallenge.Resend(
                button = word(push.resend.button, R.string.auth0_mfa_push_challenge_resend_button),
                cooldown = push.resend.cooldown ?: { seconds ->
                    resources.getString(R.string.auth0_mfa_push_challenge_resend_cooldown, seconds)
                },
            ),
            verifyAnotherWay = word(push.verifyAnotherWay, R.string.auth0_mfa_push_challenge_verify_another_way),
        ),
    )
}
