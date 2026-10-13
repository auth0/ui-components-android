package com.auth0.android.sample.ui.screens

import androidx.compose.runtime.Composable
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.EmbeddedLoginScreen
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration

@Composable
fun EmbeddedLoginDemo(
    onBack: () -> Unit,
    onAuthenticated: (Credentials) -> Unit,
    themeConfiguration: Auth0ThemeConfiguration,
) {
    EmbeddedLoginScreen(
        connection = EMBEDDED_LOGIN_CONNECTION,
        onAuthenticated = onAuthenticated,
        onExit = onBack,
        themeConfiguration = themeConfiguration,
    )
}

private const val EMBEDDED_LOGIN_CONNECTION = "Username-Password-Authentication"
