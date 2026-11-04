package com.auth0.android.sample.ui.screens

import androidx.compose.runtime.Composable
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.EmbeddedLoginScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorHandling
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration

@Composable
fun EmbeddedLogin(
    onBack: () -> Unit,
    onAuthenticated: (Credentials) -> Unit,
    onMfaError: (MfaError) -> MfaErrorHandling,
    themeConfiguration: Auth0ThemeConfiguration,
) {
    EmbeddedLoginScreen(
        connection = EMBEDDED_LOGIN_CONNECTION,
        onAuthenticated = onAuthenticated,
        onExit = onBack,
        themeConfiguration = themeConfiguration,
        onMfaError = onMfaError,
    )
}

private const val EMBEDDED_LOGIN_CONNECTION = "Username-Password-Authentication"
