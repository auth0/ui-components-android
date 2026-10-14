package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    val enabled = !isCoolingDown && !isBusy
    val label = if (isCoolingDown) {
        stringResource(R.string.embedded_login_resend_in, secondsLeft)
    } else {
        stringResource(R.string.embedded_login_resend)
    }
    val color by animateColorAsState(
        targetValue = if (enabled) Auth0Theme.colors.textBold else Auth0Theme.colors.textDisabled,
        label = "resendColor",
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = Auth0Theme.typography.body,
            color = color,
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onResend),
        )
    }
}
