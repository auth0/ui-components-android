package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginAlternativeButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginIdentifierRow
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginMethodDivider
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.theme.Auth0Theme

private const val EYE_DISABLED_ALPHA = 0.4f

/**
 * Collects the password of whoever was entered on the screen before: [identifier], shown in a read-only row under
 * [identifierLabel] with Edit to go back. [onContinue] receives the password exactly as typed, never trimmed, and the
 * field is emptied at once, whatever the answer, so a rejected password can't be sent again by a second tap and none is
 * left on screen. It is hidden again when a request starts and when the app leaves the foreground, and focused again
 * once a rejection came back.
 *
 * [errorMessage] comes from the server and hides once the user types again. With [onSendCode], Continue sits under the
 * field, then "OR" and a button for a code, which counts down while [sendCodeSecondsLeft] is above zero; without it,
 * Continue stays at the bottom. Only the button that was tapped spins ([isLoading] for Continue, [isSendingCode] for
 * the code), and both are off while either request is on its way.
 */
@Composable
internal fun LoginPasswordScreen(
    onContinue: (String) -> Unit,
    identifierLabel: String,
    identifier: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_password_title),
    isLoading: Boolean = false,
    isSendingCode: Boolean = false,
    errorMessage: String? = null,
    onEdit: (() -> Unit)? = null,
    onSendCode: (() -> Unit)? = null,
    sendCodeTitle: String = stringResource(R.string.embedded_login_send_code),
    @DrawableRes sendCodeIcon: Int = R.drawable.ic_email,
    sendCodeSecondsLeft: Int = 0,
) {
    val colors = Auth0Theme.colors
    val isBusy = isLoading || isSendingCode
    // Not saved: a secret is not kept across a rotation or a restart.
    var password by remember { mutableStateOf("") }
    var isShown by remember { mutableStateOf(false) }
    var hideError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val hiddenText = remember { PasswordVisualTransformation() }
    val activeError = errorMessage.takeUnless { hideError }

    LaunchedEffect(errorMessage) { if (errorMessage != null) hideError = false }
    // Not left readable on screen for the next attempt, or in the app switcher's snapshot. Once a request has come back
    // the field takes the focus again, ready for another try; the first time, the field takes it itself when it appears.
    var wasBusy by remember { mutableStateOf(false) }
    LaunchedEffect(isBusy) {
        if (isBusy) isShown = false else if (wasBusy) focusRequester.requestFocus()
        wasBusy = isBusy
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { isShown = false }

    val submit = {
        // Done on the keyboard bypasses the button's guards.
        if (!isBusy && password.isNotEmpty()) {
            onContinue(password)
            password = ""
        }
    }
    val showPassword = stringResource(R.string.embedded_login_show_password)
    val hidePassword = stringResource(R.string.embedded_login_hide_password)
    val passwordLabel = stringResource(R.string.embedded_login_password_label)

    FlowStepLayout(
        modifier = modifier,
        pageDescription = { FlowScreenHeader(title = title) },
        content = {
            LoginIdentifierRow(
                label = identifierLabel,
                value = identifier,
                isEditEnabled = !isBusy,
                onEdit = onEdit,
            )
            Spacer(Modifier.height(Auth0Theme.dimensions.spacingLg))
            LoginFieldLabel(passwordLabel)
            LoginInputField(
                value = password,
                // Not turned off while busy: a disabled field loses focus. What is typed meanwhile is ignored.
                onValueChange = {
                    if (!isBusy) {
                        password = it
                        hideError = true
                    }
                },
                // The visible label is a separate node, so TalkBack would land on an unnamed edit box.
                modifier = Modifier.semantics { contentDescription = passwordLabel },
                isInvalid = activeError != null,
                keyboardType = KeyboardType.Password,
                trailing = {
                    // The outlined eye, in the quiet text colour; it fades with the rest while a request is on its way.
                    IconButton(
                        onClick = { isShown = !isShown },
                        enabled = !isBusy,
                        modifier = Modifier.padding(end = Auth0Theme.dimensions.spacingSm),
                    ) {
                        Icon(
                            imageVector = if (isShown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isShown) hidePassword else showPassword,
                            tint = colors.textDefault.copy(alpha = if (isBusy) EYE_DISABLED_ALPHA else 1f),
                            modifier = Modifier.size(Auth0Theme.sizes.iconMedium),
                        )
                    }
                },
                visualTransformation = if (isShown) VisualTransformation.None else hiddenText,
                onDone = { submit() },
                focusRequester = focusRequester,
                // The keyboard stays up through the request, so a rejection does not make the screen jump twice.
                clearFocusOnDone = false,
            )
            activeError?.let { LoginInlineError(it) }
        },
        primaryAction = {
            FlowPrimaryButton(
                title = stringResource(R.string.embedded_login_continue),
                onClick = { submit() },
                isLoading = isLoading,
                enabled = password.isNotEmpty() && !isBusy,
                clearsFocus = false,
            )
        },
        secondaryAction = onSendCode?.let { sendCode ->
            {
                Column {
                    LoginMethodDivider()
                    Spacer(Modifier.height(Auth0Theme.dimensions.spacingSm))
                    LoginAlternativeButton(
                        title = if (sendCodeSecondsLeft > 0) {
                            stringResource(R.string.embedded_login_code_available_in, sendCodeTitle, sendCodeSecondsLeft)
                        } else {
                            sendCodeTitle
                        },
                        icon = sendCodeIcon,
                        onClick = sendCode,
                        enabled = !isBusy && sendCodeSecondsLeft == 0,
                        isLoading = isSendingCode,
                    )
                }
            }
        },
        // Under the field with a code on offer; alone, Continue stays at the bottom.
        pinActionsToBottom = onSendCode == null,
    )
}
