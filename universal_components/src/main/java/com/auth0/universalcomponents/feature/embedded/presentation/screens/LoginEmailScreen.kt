package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginFieldLabel
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInlineError
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginInputField
import com.auth0.universalcomponents.presentation.ui.flow.FlowPrimaryButton
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.presentation.ui.flow.flowSurface
import com.auth0.universalcomponents.theme.Auth0Theme

/** Lenient on purpose; the server has the final say. */
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Collects the email the code is sent to. [onContinue] receives the trimmed email once it looks valid.
 * [errorMessage] comes from the server and hides once the user edits the field.
 * A non-null [onContinueWithPhone] adds the "OR" divider and "Continue with Phone" below Continue, as designed.
 */
@Composable
internal fun LoginEmailScreen(
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.embedded_login_email_title),
    email: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onContinueWithPhone: (() -> Unit)? = null,
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
                    hideServerError = true
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
        secondaryAction = onContinueWithPhone?.let { switchToPhone ->
            {
                ContinueWithPhone(onClick = switchToPhone, enabled = !isLoading)
            }
        },
        // Next to Continue, as in the design with a second method; alone, Continue stays at the bottom.
        pinActionsToBottom = onContinueWithPhone == null,
    )
}

/** The "OR" divider, then the white "Continue with Phone" button. */
@Composable
private fun ContinueWithPhone(onClick: () -> Unit, enabled: Boolean) {
    val dimensions = Auth0Theme.dimensions
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSm),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Auth0Theme.colors.borderSubtle)
        Text(
            text = stringResource(R.string.embedded_login_or),
            style = Auth0Theme.typography.bodySmall,
            color = Auth0Theme.colors.textDefault,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Auth0Theme.colors.borderSubtle)
    }
    Spacer(Modifier.height(dimensions.spacingSm))
    val shape = Auth0Theme.shapes.medium
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Auth0Theme.sizes.buttonHeight)
            .flowSurface(shape, isFocused = false, isError = false)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_device),
                contentDescription = null,
                modifier = Modifier.size(dimensions.spacingMd),
                tint = if (enabled) Auth0Theme.colors.textBold else Auth0Theme.colors.textDisabled,
            )
            Spacer(Modifier.width(dimensions.spacingSm))
            Text(
                text = stringResource(R.string.embedded_login_continue_with_phone),
                style = Auth0Theme.typography.label,
                color = if (enabled) Auth0Theme.colors.textBold else Auth0Theme.colors.textDisabled,
            )
        }
    }
}
