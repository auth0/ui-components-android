package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.theme.Auth0Theme
import kotlinx.coroutines.delay

private const val RESEND_COOLDOWN_SECONDS = 60

/** "Resend Code" with a cooldown that restarts each time [sendCount] changes. */
@Composable
internal fun LoginResendButton(
    sendCount: Int,
    isBusy: Boolean,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    cooldownSeconds: Int = RESEND_COOLDOWN_SECONDS,
) {
    var secondsLeft by rememberSaveable(sendCount) { mutableIntStateOf(cooldownSeconds) }
    LaunchedEffect(sendCount) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }
    val enabled = secondsLeft == 0 && !isBusy
    val label = if (secondsLeft > 0) {
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
