package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.presentation.components.EntryStepLayout
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginAlternativeMethods
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.feature.embedded.presentation.components.rememberDismissibleError
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton

/**
 * Collects the username whose password is asked for next. A username has no format to check, so Continue turns on once
 * something is typed, and [onContinue] receives it trimmed. [errorMessage] comes from the server (for example, no
 * password step for this username), hides once the user edits the field, and comes back when the server reports again.
 * Any [otherMethods] add the "OR" divider and one "Continue with …" button each below Continue; they are off while
 * [isLoading]. On the first screen ([isEntry]) it is the entry screen, with the logo and a centered title.
 */
@Composable
internal fun LoginUsernameScreen(
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_username_title),
    username: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    otherMethods: List<LoginOption> = emptyList(),
    onChooseMethod: (LoginOption) -> Unit = {},
    isEntry: Boolean = false,
) {
    var value by rememberSaveable { mutableStateOf(username) }
    val serverError = rememberDismissibleError(errorMessage)
    val activeError = serverError.message
    val submit = {
        val trimmed = value.trim()
        // A second tap while the request is on its way is dropped.
        if (!isLoading && trimmed.isNotEmpty()) onContinue(trimmed)
    }

    EntryStepLayout(
        isEntry = isEntry,
        title = title,
        modifier = modifier,
        field = {
            LoginFieldLabel(stringResource(R.string.embedded_login_username_label))
            LoginInputField(
                value = value,
                onValueChange = {
                    value = it
                    serverError.dismiss()
                },
                isInvalid = activeError != null,
                enabled = !isLoading,
                keyboardType = KeyboardType.Text,
                onDone = submit,
            )
            activeError?.let { LoginInlineError(it) }
        },
        continueButton = {
            FlowPrimaryButton(
                title = stringResource(R.string.embedded_login_continue),
                onClick = submit,
                isLoading = isLoading,
                enabled = value.isNotBlank() && !isLoading,
            )
        },
        alternatives = otherMethods.takeIf { it.isNotEmpty() }?.let { others ->
            {
                LoginAlternativeMethods(methods = others, onChoose = onChooseMethod, enabled = !isLoading)
            }
        },
    )
}
