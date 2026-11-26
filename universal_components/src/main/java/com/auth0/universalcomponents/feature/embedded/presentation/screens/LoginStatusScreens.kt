package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.presentation.ui.flow.FlowStatusScreen
import com.auth0.universalcomponents.presentation.ui.flow.StatusIcon
import com.auth0.universalcomponents.theme.Auth0Theme

/** Shown briefly before the app moves on. */
@Composable
internal fun LoginSuccessScreen(modifier: Modifier = Modifier) {
    FlowStatusScreen(
        title = stringResource(R.string.embedded_login_success_title),
        icon = StatusIcon(Icons.Filled.Verified, Auth0Theme.colors.foregroundSuccess),
        modifier = modifier,
    )
}

/** The server offered nothing this flow can drive. A null [onBack] hides "Go back". */
@Composable
internal fun LoginNoMethodsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    FlowStatusScreen(
        title = stringResource(R.string.embedded_login_no_methods_title),
        description = stringResource(R.string.embedded_login_no_methods_description),
        actionTitle = stringResource(R.string.embedded_login_go_back),
        onAction = onBack,
        icon = StatusIcon(Icons.Filled.Block, Auth0Theme.colors.textDefault),
        modifier = modifier,
    )
}
