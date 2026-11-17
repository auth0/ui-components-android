package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginAlternativeMethods
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.feature.embedded.presentation.components.rememberDismissibleError
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout

/** Lenient on purpose; the server has the final say. */
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Collects the email the code or password is for. [onContinue] receives the trimmed email once it looks valid.
 * [errorMessage] comes from the server, hides once the user edits the field, and comes back when the server reports
 * again. Any [otherMethods] add the "OR" divider and one "Continue with …" button each below Continue, as designed; they
 * are off while [isLoading].
 */
@Composable
internal fun LoginEmailScreen(
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_email_title),
    email: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    otherMethods: List<LoginOption> = emptyList(),
    onChooseMethod: (LoginOption) -> Unit = {},
) {
    var value by rememberSaveable { mutableStateOf(email) }
    var isInvalidEmail by rememberSaveable { mutableStateOf(false) }
    val serverError = rememberDismissibleError(errorMessage)

    val activeError = if (isInvalidEmail) {
        stringResource(R.string.embedded_login_error_invalid_email)
    } else {
        serverError.message
    }
    val submit = {
        val trimmed = value.trim()
        // A second tap while the request is on its way is dropped.
        if (!isLoading) {
            if (EMAIL_PATTERN.matches(trimmed)) onContinue(trimmed) else isInvalidEmail = true
        }
    }

    FlowStepLayout(
        modifier = modifier,
        pageDescription = { FlowScreenHeader(title = title) },
        content = {
            LoginFieldLabel(stringResource(R.string.embedded_login_email_label))
            LoginInputField(
                value = value,
                onValueChange = {
                    value = it
                    isInvalidEmail = false
                    serverError.dismiss()
                },
                isInvalid = activeError != null,
                enabled = !isLoading,
                onDone = submit,
            )
            activeError?.let { LoginInlineError(it) }
        },
        primaryAction = {
            FlowPrimaryButton(
                title = stringResource(R.string.embedded_login_continue),
                onClick = submit,
                isLoading = isLoading,
                enabled = value.isNotBlank() && !isLoading,
            )
        },
        secondaryAction = otherMethods.takeIf { it.isNotEmpty() }?.let { others ->
            {
                LoginAlternativeMethods(methods = others, onChoose = onChooseMethod, enabled = !isLoading)
            }
        },
        // Next to Continue, as in the design with a second method; alone, Continue stays at the bottom.
        pinActionsToBottom = otherMethods.isEmpty(),
    )
}
