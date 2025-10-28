package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.auth0.android.ui_components.presentation.navigation.AuthenticatorSettingsNavigationHost

@Composable
public fun AuthenticatorSettingsComponent(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    AuthenticatorSettingsNavigationHost(
        navController
    )
}