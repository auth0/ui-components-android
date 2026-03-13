package com.auth0.android.sample.ui.screens

import androidx.compose.runtime.Composable
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorSettingsComponent
import com.auth0.android.ui_components.theme.Auth0ThemeConfiguration

@Composable
fun Settings(themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default) {
    AuthenticatorSettingsComponent(themeConfiguration = themeConfiguration)
}
