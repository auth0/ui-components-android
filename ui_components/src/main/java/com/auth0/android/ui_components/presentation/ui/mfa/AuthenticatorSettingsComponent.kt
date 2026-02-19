package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.auth0.android.ui_components.presentation.navigation.AuthenticatorSettingsNavigationHost
import com.auth0.android.ui_components.theme.Auth0Theme
import com.auth0.android.ui_components.theme.Auth0ThemeConfiguration

/**
 * Auth0 Authenticator Settings Component.
 *
 * Provides a complete UI for managing MFA authenticators including enrollment,
 * verification, and management of various authenticator types.
 *
 * @param modifier Optional modifier for the component
 * @param themeConfiguration Optional theme configuration for customizing colors,
 *                          typography, shapes, and spacing. Defaults to Auth0 standard theme.
 *
 * Example usage:
 * ```kotlin
 * // Default Auth0 theme
 * AuthenticatorSettingsComponent()
 *
 * // Custom brand colors
 * AuthenticatorSettingsComponent(
 *     themeConfiguration = Auth0ThemeConfiguration(
 *         color = Auth0Color.light().copy(
 *             primary = Color(0xFFFF6B00)  // Custom orange
 *         )
 *     )
 * )
 *
 * // Dark mode
 * AuthenticatorSettingsComponent(
 *     themeConfiguration = Auth0ThemeConfiguration(
 *         color = Auth0Color.dark()
 *     )
 * )
 * ```
 */
@Composable
public fun AuthenticatorSettingsComponent(
    modifier: Modifier = Modifier,
    themeConfiguration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default
) {
    val navController = rememberNavController()

    Auth0Theme(configuration = themeConfiguration) {
        AuthenticatorSettingsNavigationHost(
            navController
        )
    }
}