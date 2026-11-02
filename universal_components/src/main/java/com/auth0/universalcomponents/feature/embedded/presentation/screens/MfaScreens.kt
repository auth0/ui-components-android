package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginPrimaryButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenHeader
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginStepLayout
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginTextButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.MfaFactorRow
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.theme.Auth0Theme

/** "Choose a device": every second factor the server offered, one row each. */
@Composable
internal fun MfaFactorListScreen(
    factors: List<MfaFactor>,
    onSelect: (MfaFactor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = Auth0Theme.dimensions
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
                title = stringResource(R.string.embedded_login_mfa_list_title),
                subtitle = stringResource(R.string.embedded_login_mfa_list_subtitle),
                bottomSpacing = dimensions.spacingLg,
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingXs)) {
                factors.forEach { factor ->
                    MfaFactorRow(icon = factor.icon(), label = factor.label(), onClick = { onSelect(factor) })
                }
            }
        },
        primaryAction = {},
    )
}

/**
 * Waiting for the user to approve the push on their device. Resend opens once [resendSecondsLeft] reaches zero;
 * a null [onChooseAnotherDevice] hides that action. [isSending] keeps both off until the push is out.
 */
@Composable
internal fun MfaPushScreen(
    deviceName: String,
    resendSecondsLeft: Int,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    isSending: Boolean = false,
    onChooseAnotherDevice: (() -> Unit)? = null,
) {
    val dimensions = Auth0Theme.dimensions
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
                title = stringResource(R.string.embedded_login_mfa_push_title),
                subtitle = stringResource(R.string.embedded_login_mfa_push_subtitle, deviceName),
                bottomSpacing = dimensions.spacingXxl + dimensions.spacingMd,
            )
        },
        content = { MfaWaitingForApproval() },
        primaryAction = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingMd)) {
                LoginPrimaryButton(
                    title = if (resendSecondsLeft > 0) {
                        stringResource(R.string.embedded_login_resend_in, resendSecondsLeft)
                    } else {
                        stringResource(R.string.embedded_login_resend)
                    },
                    onClick = onResend,
                    enabled = !isSending && resendSecondsLeft == 0,
                )
                if (onChooseAnotherDevice != null) {
                    LoginTextButton(
                        title = stringResource(R.string.embedded_login_mfa_choose_another_device),
                        onClick = onChooseAnotherDevice,
                        enabled = !isSending,
                    )
                }
            }
        },
        pinActionsToBottom = false,
    )
}

/** The spinner with "Waiting for approval", announced once to screen readers. */
@Composable
private fun MfaWaitingForApproval() {
    val dimensions = Auth0Theme.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.spacingXl + dimensions.spacingXxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSm),
    ) {
        CircularLoader(modifier = Modifier.size(dimensions.spacingXxl))
        Text(
            text = stringResource(R.string.embedded_login_mfa_waiting),
            style = Auth0Theme.typography.body,
            color = Auth0Theme.colors.textDefault,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** The device's own name, so two phones with push never look like the same row. */
private fun MfaFactor.label(): String = when (this) {
    is MfaFactor.Push -> deviceName
}

@Composable
private fun MfaFactor.icon(): Painter = when (this) {
    is MfaFactor.Push -> painterResource(R.drawable.ic_device)
}
