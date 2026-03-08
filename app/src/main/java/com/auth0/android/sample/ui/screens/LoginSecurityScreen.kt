package com.auth0.android.sample.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorSettingsComponent
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 *
 * Shows enrolled sign-in methods and MFA factor list.
 *
 * @param onBack Navigate back
 * @param onManageMfa Navigate to existing MFA settings (AuthenticatorSettingsComponent)
 */
@Composable
fun LoginSecurityScreen(
    onBack: () -> Unit,
    onManageMfa: () -> Unit = {}
) {
    val colors = Auth0Theme.colors

    Scaffold(
//        topBar = {
//            TopBar(title = "", showBackNavigation = true, onBackClick = onBack)
//        },
        containerColor = colors.backgroundLayerBase
    ) { padding ->
        AuthenticatorSettingsComponent()
    }
}
