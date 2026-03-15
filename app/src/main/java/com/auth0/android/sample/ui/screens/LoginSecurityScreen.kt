package com.auth0.android.sample.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.auth0.universalcomponents.presentation.ui.mfa.AuthenticatorSettingsComponent
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.theme.Auth0ThemeConfiguration

/**
 *
 * Shows enrolled sign-in methods and MFA factor list.
 *
 * @param onBack Navigate back
 * @param onManageMfa Navigate to existing MFA settings (AuthenticatorSettingsComponent)
 * @param themeConfiguration Theme configuration to apply to the MFA component
 */
@Composable
fun LoginSecurityScreen(
    onBack: () -> Unit,
    onManageMfa: () -> Unit = {},
    themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default
) {
    val colors = Auth0Theme.colors

    Scaffold(
        containerColor = colors.backgroundLayerBase
    ) { _ ->
        AuthenticatorSettingsComponent(themeConfiguration = themeConfiguration)
    }
}
