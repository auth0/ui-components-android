package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.CODE_LENGTH
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginCodeField
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginPrimaryButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginResendButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenHeader
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginStepLayout

/**
 * Code entry, shared by the emailed code and the MFA step-up. Verify turns on once every digit is in.
 * [errorMessage] hides once the user types again. A null [onEdit] or [onResend] hides that action.
 * [isResending] keeps Verify off without spinning it while a new code is on its way.
 * [canResendNow] skips the resend cooldown, for when the current code can no longer be used.
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
    canResendNow: Boolean = false,
    errorMessage: String? = null,
    onEdit: (() -> Unit)? = null,
    onResend: (() -> Unit)? = null,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var hideError by rememberSaveable { mutableStateOf(false) }
    var sendCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(errorMessage) { if (errorMessage != null) hideError = false }
    val activeError = errorMessage.takeUnless { hideError }

    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
                title = title,
                subtitle = subtitle,
                editLabel = stringResource(R.string.embedded_login_code_edit),
                onEdit = onEdit,
            )
        },
        content = {
            LoginFieldLabel(fieldLabel)
            LoginCodeField(
                code = code,
                onCodeChange = {
                    code = it
                    hideError = true
                },
                isError = activeError != null,
                length = digitCount,
            )
            activeError?.let { LoginInlineError(it) }
        },
        primaryAction = {
            LoginPrimaryButton(
                title = stringResource(R.string.embedded_login_verify),
                onClick = { onVerify(code) },
                isLoading = isLoading,
                enabled = code.length == digitCount && !isLoading && !isResending,
            )
        },
        secondaryAction = onResend?.let { resend ->
            {
                LoginResendButton(
                    sendCount = sendCount,
                    isBusy = isLoading || isResending,
                    skipCooldown = canResendNow,
                    onResend = {
                        sendCount++
                        resend()
                    },
                )
            }
        },
        pinActionsToBottom = false,
    )
}
