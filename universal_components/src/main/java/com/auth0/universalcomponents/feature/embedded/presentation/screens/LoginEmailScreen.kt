package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginPrimaryButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenHeader
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginStepLayout

/** Lenient on purpose; the server has the final say. */
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Collects the email the code is sent to. [onContinue] receives the trimmed email once it looks valid.
 * [errorMessage] comes from the server and hides once the user edits the field.
 */
@Composable
fun LoginEmailScreen(
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_email_title),
    email: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    var value by rememberSaveable { mutableStateOf(email) }
    var isInvalidEmail by rememberSaveable { mutableStateOf(false) }
    var hideServerError by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(errorMessage) { if (errorMessage != null) hideServerError = false }

    val activeError = if (isInvalidEmail) {
        stringResource(R.string.embedded_login_error_invalid_email)
    } else {
        errorMessage.takeUnless { hideServerError }
    }
    val submit = {
        val trimmed = value.trim()
        if (EMAIL_PATTERN.matches(trimmed)) onContinue(trimmed) else isInvalidEmail = true
    }

    LoginStepLayout(
        modifier = modifier,
        pageDescription = { LoginScreenHeader(title = title) },
        content = {
            LoginFieldLabel(stringResource(R.string.embedded_login_email_label))
            LoginInputField(
                value = value,
                onValueChange = {
                    value = it
                    isInvalidEmail = false
                    hideServerError = true
                },
                isInvalid = activeError != null,
                enabled = !isLoading,
                onDone = submit,
            )
            activeError?.let { LoginInlineError(it) }
        },
        primaryAction = {
            LoginPrimaryButton(
                title = stringResource(R.string.embedded_login_continue),
                onClick = submit,
                isLoading = isLoading,
                enabled = value.isNotBlank() && !isLoading,
            )
        },
    )
}
