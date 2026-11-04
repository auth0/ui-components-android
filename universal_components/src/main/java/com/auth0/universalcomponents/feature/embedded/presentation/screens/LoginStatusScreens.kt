package com.auth0.universalcomponents.feature.embedded.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.presentation.ui.flow.LoginStatusScreen

/** Shown briefly before the app moves on. */
@Composable
internal fun LoginSuccessScreen(modifier: Modifier = Modifier) {
    LoginStatusScreen(title = stringResource(R.string.embedded_login_success_title), modifier = modifier)
}

/** The server offered nothing this flow can drive. A null [onBack] hides "Go back". */
@Composable
internal fun LoginNoMethodsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    LoginStatusScreen(
        title = stringResource(R.string.embedded_login_no_methods_title),
        description = stringResource(R.string.embedded_login_no_methods_description),
        actionTitle = stringResource(R.string.embedded_login_go_back),
        onAction = onBack,
        modifier = modifier,
    )
}
