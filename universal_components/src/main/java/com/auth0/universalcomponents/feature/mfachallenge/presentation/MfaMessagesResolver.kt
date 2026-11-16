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
        phoneChallenge = phoneChallenge.resolve(resources),
        emailChallenge = emailChallenge.resolve(resources),
        totpChallenge = totpChallenge.resolve(resources),
        recoveryCodeChallenge = recoveryCodeChallenge.resolve(resources),
    )
}

/** The words of the phone code screen and its list rows, resolved the same way as the rest. */
private fun MfaMessages.PhoneChallenge.resolve(resources: Resources): ResolvedMfaMessages.PhoneChallenge {
    fun word(override: String?, @StringRes id: Int) = override ?: resources.getString(id)

    return ResolvedMfaMessages.PhoneChallenge(
        title = word(title, R.string.auth0_mfa_phone_challenge_title),
        description = description ?: { number ->
            // The end of the number, not all of it: a server that hides every digit leaves nothing to name.
            number.endingDigits()?.let { resources.getString(R.string.auth0_mfa_phone_challenge_description, it) }
                ?: resources.getString(R.string.auth0_mfa_phone_challenge_description_hidden)
        },
        descriptionVoice = descriptionVoice ?: { number ->
            number.endingDigits()?.let { resources.getString(R.string.auth0_mfa_phone_challenge_description_voice, it) }
                ?: resources.getString(R.string.auth0_mfa_phone_challenge_description_voice_hidden)
        },
        codeLabel = word(codeLabel, R.string.auth0_mfa_phone_challenge_code_label),
        verifyButton = word(verifyButton, R.string.auth0_mfa_phone_challenge_verify_button),
        wrongCode = word(wrongCode, R.string.auth0_mfa_phone_challenge_wrong_code),
        expiredCode = word(expiredCode, R.string.auth0_mfa_phone_challenge_expired_code),
        noConnection = word(noConnection, R.string.auth0_mfa_phone_challenge_no_connection),
        tooManyRequests = word(tooManyRequests, R.string.auth0_mfa_phone_challenge_too_many_requests),
        resend = ResolvedMfaMessages.PhoneChallenge.Resend(
            button = word(resend.button, R.string.auth0_mfa_phone_challenge_resend_button),
            cooldown = resend.cooldown ?: { seconds ->
                resources.getString(R.string.auth0_mfa_phone_challenge_resend_cooldown, seconds)
            },
        ),
        verifyAnotherWay = word(verifyAnotherWay, R.string.auth0_mfa_phone_challenge_verify_another_way),
        rowText = rowText ?: { resources.getString(R.string.auth0_mfa_phone_challenge_row_text) },
        rowVoice = rowVoice ?: { resources.getString(R.string.auth0_mfa_phone_challenge_row_voice) },
    )
}

/** The words of the email code screen and its list row, resolved the same way as the rest. */
private fun MfaMessages.EmailChallenge.resolve(resources: Resources): ResolvedMfaMessages.EmailChallenge {
    fun word(override: String?, @StringRes id: Int) = override ?: resources.getString(id)

    return ResolvedMfaMessages.EmailChallenge(
        row = ResolvedMfaMessages.Message(
            word(row.title, R.string.auth0_mfa_email_challenge_row_title),
            word(row.description, R.string.auth0_mfa_email_challenge_row_description),
        ),
        title = word(title, R.string.auth0_mfa_email_challenge_title),
        description = description ?: { address ->
            resources.getString(R.string.auth0_mfa_email_challenge_description, address)
        },
        descriptionUnnamed = word(descriptionUnnamed, R.string.auth0_mfa_email_challenge_description_unnamed),
        codeLabel = word(codeLabel, R.string.auth0_mfa_email_challenge_code_label),
        verifyButton = word(verifyButton, R.string.auth0_mfa_email_challenge_verify_button),
        wrongCode = word(wrongCode, R.string.auth0_mfa_email_challenge_wrong_code),
        expiredCode = word(expiredCode, R.string.auth0_mfa_email_challenge_expired_code),
        noConnection = word(noConnection, R.string.auth0_mfa_email_challenge_no_connection),
        tooManyRequests = word(tooManyRequests, R.string.auth0_mfa_email_challenge_too_many_requests),
        resend = ResolvedMfaMessages.EmailChallenge.Resend(
            button = word(resend.button, R.string.auth0_mfa_email_challenge_resend_button),
            cooldown = resend.cooldown ?: { seconds ->
                resources.getString(R.string.auth0_mfa_email_challenge_resend_cooldown, seconds)
            },
        ),
        verifyAnotherWay = word(verifyAnotherWay, R.string.auth0_mfa_email_challenge_verify_another_way),
    )
}

/** The words of the authenticator app code screen and its list row, resolved the same way as the rest. */
private fun MfaMessages.TotpChallenge.resolve(resources: Resources): ResolvedMfaMessages.TotpChallenge {
    fun word(override: String?, @StringRes id: Int) = override ?: resources.getString(id)

    return ResolvedMfaMessages.TotpChallenge(
        row = ResolvedMfaMessages.Message(
            word(row.title, R.string.auth0_mfa_totp_challenge_row_title),
            word(row.description, R.string.auth0_mfa_totp_challenge_row_description),
        ),
        title = word(title, R.string.auth0_mfa_totp_challenge_title),
        description = word(description, R.string.auth0_mfa_totp_challenge_description),
        codeLabel = word(codeLabel, R.string.auth0_mfa_totp_challenge_code_label),
        verifyButton = word(verifyButton, R.string.auth0_mfa_totp_challenge_verify_button),
        wrongCode = word(wrongCode, R.string.auth0_mfa_totp_challenge_wrong_code),
        noConnection = word(noConnection, R.string.auth0_mfa_totp_challenge_no_connection),
        verifyAnotherWay = word(verifyAnotherWay, R.string.auth0_mfa_totp_challenge_verify_another_way),
    )
}

/** The words of the recovery code screens (typing it, then saving the new one) and its list row, resolved the same way. */
private fun MfaMessages.RecoveryCodeChallenge.resolve(resources: Resources): ResolvedMfaMessages.RecoveryCodeChallenge {
    fun word(override: String?, @StringRes id: Int) = override ?: resources.getString(id)

    return ResolvedMfaMessages.RecoveryCodeChallenge(
        row = ResolvedMfaMessages.Message(
            word(row.title, R.string.auth0_mfa_recovery_code_challenge_row_title),
            word(row.description, R.string.auth0_mfa_recovery_code_challenge_row_description),
        ),
        title = word(title, R.string.auth0_mfa_recovery_code_challenge_title),
        description = word(description, R.string.auth0_mfa_recovery_code_challenge_description),
        codeLabel = word(codeLabel, R.string.auth0_mfa_recovery_code_challenge_code_label),
        placeholder = word(placeholder, R.string.auth0_mfa_recovery_code_challenge_placeholder),
        continueButton = word(continueButton, R.string.auth0_mfa_recovery_code_challenge_continue_button),
        wrongCode = word(wrongCode, R.string.auth0_mfa_recovery_code_challenge_wrong_code),
        noConnection = word(noConnection, R.string.auth0_mfa_recovery_code_challenge_no_connection),
        verifyAnotherWay = word(verifyAnotherWay, R.string.auth0_mfa_recovery_code_challenge_verify_another_way),
        showCode = word(showCode, R.string.auth0_mfa_recovery_code_challenge_show_code),
        hideCode = word(hideCode, R.string.auth0_mfa_recovery_code_challenge_hide_code),
        saveTitle = word(saveTitle, R.string.auth0_mfa_recovery_code_challenge_save_title),
        saveDescription = word(saveDescription, R.string.auth0_mfa_recovery_code_challenge_save_description),
        saveHelper = word(saveHelper, R.string.auth0_mfa_recovery_code_challenge_save_helper),
        savedConfirmation = word(savedConfirmation, R.string.auth0_mfa_recovery_code_challenge_saved_confirmation),
        copied = word(copied, R.string.auth0_mfa_recovery_code_challenge_copied),
        copyCode = word(copyCode, R.string.auth0_mfa_recovery_code_challenge_copy_code),
    )
}

/**
 * The server hides most of a phone number (`XXXXXXXXX0128`, or with `*` or other look-alikes). The default words keep
 * the digits and the usual phone marks and draw every other character as one `*`, as the design does, so the hidden part
 * is a single glyph: a mix of characters would show at different sizes and heights. The lines that show the number also
 * turn off the font's contextual alternates (`NO_CONTEXTUAL_ALTERNATES`).
 */
internal fun String.forDisplay(): String =
    map { if (it.isDigit() || it in PHONE_MARKS) it else HIDDEN_DIGIT }.joinToString("")

private const val PHONE_MARKS = "+-() ."

private const val HIDDEN_DIGIT = '*'

/** A number the server hid completely (`*********`) has nothing to show, so the default words don't name it. */
internal fun String.hasDigits(): Boolean = any(Char::isDigit)

/**
 * The end of a phone number, as the code screen names it ("ending in 900"): the last [count] digits the server really
 * shows. Digits before a hidden one are left out, since they are not the end of the number: `(4**)***-*900` gives `900`,
 * the server showing only three, and `+14155550123` typed in full gives `0123`. Anything but a digit or a phone mark counts
 * as hidden, as in [forDisplay]. Null when no digit is shown at the end (`*****`, or an empty string).
 */
internal fun String.endingDigits(count: Int = ENDING_DIGITS): String? {
    val shown = substring(indexOfLast { !(it in '0'..'9' || it in PHONE_MARKS) } + 1)
    return shown.filter { it in '0'..'9' }.takeLast(count).ifEmpty { null }
}

private const val ENDING_DIGITS = 4
