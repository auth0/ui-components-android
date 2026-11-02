package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.theme.Auth0Theme

/** "Resend Code", or the wait left while [secondsLeft] counts down; the ViewModel owns the countdown. */
@Composable
internal fun LoginResendButton(
    secondsLeft: Int,
    isBusy: Boolean,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCoolingDown = secondsLeft > 0
    LoginTextButton(
        title = if (isCoolingDown) {
            stringResource(R.string.embedded_login_resend_in, secondsLeft)
        } else {
            stringResource(R.string.embedded_login_resend)
        },
        onClick = onResend,
        modifier = modifier,
        enabled = !isCoolingDown && !isBusy,
        style = Auth0Theme.typography.body,
    )
}
