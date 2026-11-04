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
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginPrimaryButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenHeader
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginStepLayout
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginTextButton
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.mfachallenge.presentation.components.MfaFactorRow
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Every factor the round offers, one row each. From round 2 on, the server has asked for one more factor, so the
 * header says so. While [sending]'s challenge goes out, its row is busy and no row can be tapped.
 */
@Composable
internal fun MfaFactorListScreen(
    factors: List<MfaFactor>,
    onSelect: (MfaFactor) -> Unit,
    modifier: Modifier = Modifier,
    isAdditionalRound: Boolean = false,
    sending: MfaFactor? = null,
) {
    val dimensions = Auth0Theme.dimensions
    val title = if (isAdditionalRound) {
        R.string.embedded_login_mfa_list_title_additional
    } else {
        R.string.embedded_login_mfa_list_title
    }
    val subtitle = if (isAdditionalRound) {
        R.string.embedded_login_mfa_list_subtitle_additional
    } else {
        R.string.embedded_login_mfa_list_subtitle
    }
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
                title = stringResource(title),
                subtitle = stringResource(subtitle),
                bottomSpacing = dimensions.spacingLg,
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingXs)) {
                factors.forEach { factor ->
                    MfaFactorRow(
                        icon = factor.icon(),
                        label = factor.label(),
                        onClick = { onSelect(factor) },
                        isBusy = factor == sending,
                        enabled = sending == null,
                    )
                }
            }
        },
        primaryAction = {},
    )
}

/**
 * Waiting for the user to approve the push on [deviceName], which may be blank. Resend shows only while
 * [canResend] (the server allows another push) and opens once [resendSecondsLeft] reaches zero; while the push is
 * being sent again it's gone, so it can't be tapped twice, and [isResending] keeps "Verify another way" off until the
 * push is out. A null [onVerifyAnotherWay] hides that action.
 */
@Composable
internal fun MfaPushScreen(
    deviceName: String,
    canResend: Boolean,
    resendSecondsLeft: Int,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    isResending: Boolean = false,
    onVerifyAnotherWay: (() -> Unit)? = null,
) {
    val dimensions = Auth0Theme.dimensions
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
                title = stringResource(R.string.embedded_login_mfa_push_title),
                subtitle = if (deviceName.isBlank()) {
                    stringResource(R.string.embedded_login_mfa_push_subtitle_unnamed)
                } else {
                    stringResource(R.string.embedded_login_mfa_push_subtitle, deviceName)
                },
                bottomSpacing = dimensions.spacingXxl + dimensions.spacingMd,
            )
        },
        content = { MfaWaitingForApproval() },
        primaryAction = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingMd)) {
                if (canResend) {
                    LoginPrimaryButton(
                        title = if (resendSecondsLeft > 0) {
                            stringResource(R.string.embedded_login_mfa_resend_in, resendSecondsLeft)
                        } else {
                            stringResource(R.string.embedded_login_mfa_resend)
                        },
                        onClick = onResend,
                        enabled = resendSecondsLeft == 0,
                    )
                }
                if (onVerifyAnotherWay != null) {
                    LoginTextButton(
                        title = stringResource(R.string.embedded_login_mfa_verify_another_way),
                        onClick = onVerifyAnotherWay,
                        enabled = !isResending,
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

/** The device's own name, so two phones with push never look like the same row; the factor's name if it has none. */
@Composable
private fun MfaFactor.label(): String = when (this) {
    is MfaFactor.Push -> deviceName.ifBlank { stringResource(R.string.embedded_login_mfa_push_factor) }
}

@Composable
private fun MfaFactor.icon(): Painter = when (this) {
    is MfaFactor.Push -> painterResource(R.drawable.ic_device)
}
