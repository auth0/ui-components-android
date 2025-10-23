package com.auth0.android.sample.ui.screens

import androidx.compose.runtime.Composable
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorSettingsComponent

@Composable
fun Settings() {
    // Simple - no navigation configuration needed!
    // All MFA navigation is handled internally within the UI components module
    AuthenticatorSettingsComponent()
}