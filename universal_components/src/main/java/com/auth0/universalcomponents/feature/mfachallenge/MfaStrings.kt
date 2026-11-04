package com.auth0.universalcomponents.feature.mfachallenge

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.auth0.universalcomponents.R

/** Where a default word comes from: a string resource formatted with args. Apart from Android so tests can fake it. */
internal fun interface MfaStrings {
    fun get(@StringRes id: Int, vararg args: Any): String
}

/**
 * The words in the language the screen is in now: read again when the language changes. Pass them to a standard screen
 * you use on its own (they default to this class's defaults), or read them in a screen of your own.
 */
@Composable
fun MfaMessages.resolve(): ResolvedMfaMessages {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(this, context, configuration) {
        // At most one argument, so the copy the spread makes is nothing.
        @Suppress("SpreadOperator")
        resolve(MfaStrings { id, args -> context.resources.getString(id, *args) })
    }
}

/** The app's words where it set some, the string resources' where not. */
internal fun MfaMessages.resolve(strings: MfaStrings): ResolvedMfaMessages {
    fun word(override: String?, @StringRes id: Int) = override ?: strings.get(id)

    fun text(override: MfaMessages.Text, @StringRes title: Int, @StringRes description: Int) =
        ResolvedMfaMessages.Text(word(override.title, title), word(override.description, description))

    val headers = authenticatorList.headers
    val push = pushChallenge
    return ResolvedMfaMessages(
        authenticatorList = ResolvedMfaMessages.AuthenticatorList(
            ResolvedMfaMessages.AuthenticatorList.Headers(
                challenge = text(
                    headers.challenge,
                    R.string.auth0_mfa_authenticator_list_headers_challenge_title,
                    R.string.auth0_mfa_authenticator_list_headers_challenge_description,
                ),
                challengeAdditional = text(
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
            texts = mapOf(
                MfaError.Cancelled::class.java to text(
                    errors.cancelled,
                    R.string.auth0_mfa_errors_cancelled_title,
                    R.string.auth0_mfa_errors_cancelled_description,
                ),
                MfaError.TokenExpired::class.java to text(
                    errors.tokenExpired,
                    R.string.auth0_mfa_errors_token_expired_title,
                    R.string.auth0_mfa_errors_token_expired_description,
                ),
                MfaError.TokenInvalid::class.java to text(
                    errors.tokenInvalid,
                    R.string.auth0_mfa_errors_token_invalid_title,
                    R.string.auth0_mfa_errors_token_invalid_description,
                ),
                MfaError.Rejected::class.java to text(
                    errors.rejected,
                    R.string.auth0_mfa_errors_rejected_title,
                    R.string.auth0_mfa_errors_rejected_description,
                ),
                MfaError.TooManyAttempts::class.java to text(
                    errors.tooManyAttempts,
                    R.string.auth0_mfa_errors_too_many_attempts_title,
                    R.string.auth0_mfa_errors_too_many_attempts_description,
                ),
                MfaError.NoEligibleFactors::class.java to text(
                    errors.noEligibleFactors,
                    R.string.auth0_mfa_errors_no_eligible_factors_title,
                    R.string.auth0_mfa_errors_no_eligible_factors_description,
                ),
                MfaError.Network::class.java to text(
                    errors.network,
                    R.string.auth0_mfa_errors_network_title,
                    R.string.auth0_mfa_errors_network_description,
                ),
                MfaError.Unexpected::class.java to text(
                    errors.unexpected,
                    R.string.auth0_mfa_errors_unexpected_title,
                    R.string.auth0_mfa_errors_unexpected_description,
                ),
            ),
        ),
        pushChallenge = ResolvedMfaMessages.PushChallenge(
            row = text(
                push.row,
                R.string.auth0_mfa_push_challenge_row_title,
                R.string.auth0_mfa_push_challenge_row_description,
            ),
            title = word(push.title, R.string.auth0_mfa_push_challenge_title),
            description = push.description ?: { device ->
                strings.get(R.string.auth0_mfa_push_challenge_description, device)
            },
            descriptionUnnamed = word(push.descriptionUnnamed, R.string.auth0_mfa_push_challenge_description_unnamed),
            waiting = word(push.waiting, R.string.auth0_mfa_push_challenge_waiting),
            resend = ResolvedMfaMessages.PushChallenge.Resend(
                button = word(push.resend.button, R.string.auth0_mfa_push_challenge_resend_button),
                cooldown = push.resend.cooldown ?: { seconds ->
                    strings.get(R.string.auth0_mfa_push_challenge_resend_cooldown, seconds)
                },
            ),
            verifyAnotherWay = word(push.verifyAnotherWay, R.string.auth0_mfa_push_challenge_verify_another_way),
        ),
    )
}
