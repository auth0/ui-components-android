package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.CODE_LENGTH
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginAlternativeButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginCodeField
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginMethodDivider
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginResendButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Entry for the emailed or texted code. Verify turns on once every digit is in.
 * [errorMessage] hides once the user types again, unless [needsNewCode]: a code that can't be checked any more keeps its
 * message while the user types, keeps Verify off, opens Resend at once, and is cleared from the boxes when Resend is
 * tapped. A null [onEdit], [onResend] or [onUsePassword] hides that action.
 * [isResending] and [isOpeningPassword] lock the boxes and Verify while a new code or the password is on its way; only
 * the button that was tapped spins. [resendSecondsLeft] is the wait before Resend opens.
 * [onUsePassword] adds "OR" and "Continue with a password" under Resend.
 */
@Composable
internal fun LoginCodeScreen(
    onVerify: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_code_title),
    subtitle: String? = null,
    fieldLabel: String = stringResource(R.string.embedded_login_code_label),
    digitCount: Int = CODE_LENGTH,
    isLoading: Boolean = false,
    isResending: Boolean = false,
    isOpeningPassword: Boolean = false,
    resendSecondsLeft: Int = 0,
    errorMessage: String? = null,
    needsNewCode: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onResend: (() -> Unit)? = null,
    onUsePassword: (() -> Unit)? = null,
) {
    var code by rememberSaveable { mutableStateOf("") }
    // Hidden once the user types after a refused code, shown again by the next Verify. Kept across a rotation, so a
    // line the user already dismissed does not come back.
    var hideError by rememberSaveable { mutableStateOf(false) }
    val activeError = errorMessage.takeUnless { hideError && !needsNewCode }
    // Some request is on its way: the boxes and Verify lock, and only the button that was tapped shows it.
    val isBusy = isLoading || isResending || isOpeningPassword

    FlowStepLayout(
        modifier = modifier,
        pageDescription = {
            FlowScreenHeader(
                title = title,
                subtitle = subtitle,
                editLabel = stringResource(R.string.embedded_login_code_edit),
                onEdit = onEdit,
                isEditEnabled = !isBusy,
            )
        },
        content = {
            LoginFieldLabel(fieldLabel)
            LoginCodeField(
                code = code,
                // Not turned off while busy, as a disabled field loses focus; what is typed meanwhile is ignored.
                onCodeChange = {
                    if (!isBusy) {
                        code = it
                        hideError = true
                    }
                },
                isError = activeError != null,
                length = digitCount,
            )
            activeError?.let { LoginInlineError(it) }
        },
        primaryAction = {
            FlowPrimaryButton(
                title = stringResource(R.string.embedded_login_verify),
                onClick = {
                    hideError = false
                    onVerify(code)
                },
                isLoading = isLoading,
                enabled = code.length == digitCount && !isBusy && !needsNewCode,
            )
        },
        secondaryAction = if (onResend != null || onUsePassword != null) {
            {
                Column {
                    if (onResend != null) {
                        LoginResendButton(
                            secondsLeft = resendSecondsLeft,
                            isBusy = isBusy,
                            onResend = {
                                // The old code is no use once a new one is on its way.
                                if (needsNewCode) code = ""
                                onResend()
                            },
                        )
                    }
                    if (onUsePassword != null) {
                        Spacer(Modifier.height(Auth0Theme.dimensions.spacingMd))
                        LoginMethodDivider()
                        Spacer(Modifier.height(Auth0Theme.dimensions.spacingMd))
                        LoginAlternativeButton(
                            title = stringResource(R.string.embedded_login_use_password),
                            icon = R.drawable.ic_password,
                            onClick = onUsePassword,
                            enabled = !isBusy,
                            isLoading = isOpeningPassword,
                        )
                    }
                }
            }
        } else {
            null
        },
        pinActionsToBottom = false,
    )
}
