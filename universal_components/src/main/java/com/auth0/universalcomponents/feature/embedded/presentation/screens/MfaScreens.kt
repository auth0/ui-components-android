package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenHeader
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginStepLayout
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginTextButton
import com.auth0.universalcomponents.feature.embedded.presentation.components.MfaFactorRow
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.theme.Auth0Theme

/** "Verify it's you": every second factor the server offered, one row each. */
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
 * Waiting for the user to approve the push on their device. A null [onUseAnotherMethod] hides that action;
 * [isSending] keeps it off until the push is out.
 */
@Composable
internal fun MfaPushScreen(
    deviceName: String,
    modifier: Modifier = Modifier,
    isSending: Boolean = false,
    onUseAnotherMethod: (() -> Unit)? = null,
) {
    LoginStepLayout(
        modifier = modifier,
        pageDescription = {
            LoginScreenHeader(
                title = stringResource(R.string.embedded_login_mfa_push_title),
                subtitle = stringResource(R.string.embedded_login_mfa_push_subtitle, deviceName),
            )
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularLoader(modifier = Modifier.size(Auth0Theme.sizes.iconLarge))
            }
        },
        primaryAction = {
            if (onUseAnotherMethod != null) {
                LoginTextButton(
                    title = stringResource(R.string.embedded_login_mfa_use_another_method),
                    onClick = onUseAnotherMethod,
                    enabled = !isSending,
                )
            }
        },
        pinActionsToBottom = false,
    )
}

/** Names the device, so two phones with push never look like the same row. */
@Composable
private fun MfaFactor.label(): String = when (this) {
    is MfaFactor.Push -> stringResource(R.string.embedded_login_mfa_factor_push, deviceName)
}

@Composable
private fun MfaFactor.icon(): Painter = when (this) {
    is MfaFactor.Push -> rememberVectorPainter(Icons.Outlined.NotificationsActive)
}
