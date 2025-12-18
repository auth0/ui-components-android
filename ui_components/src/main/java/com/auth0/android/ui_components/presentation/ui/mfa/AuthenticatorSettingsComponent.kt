package com.auth0.android.ui_components.presentation.ui.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.auth0.android.ui_components.presentation.navigation.AuthenticatorSettingsNavigationHost
import com.auth0.android.ui_components.theme.Auth0UiTheme
import com.auth0.android.ui_components.theme.Auth0UiThemeConfig

/**
 * Main entry point for the Auth0 Authenticator Settings UI.
 *
 * This composable displays the multi-factor authentication settings screens,
 * allowing users to manage their enrolled authenticators.
 *
 * ## Theming
 *
 * The component uses its own internal theme system. You can customize the appearance
 * by providing a [Auth0UiThemeConfig] instance.
 *
 * ### Default (no customization):
 * ```
 * AuthenticatorSettingsComponent()
 * ```
 *
 * ### Custom colors:
 * ```
 * AuthenticatorSettingsComponent(
 *     themeConfig = Auth0UiThemeConfig(
 *         colors = Auth0ColorScheme(
 *             primary = Color(0xFF6200EE)
 *         )
 *     )
 * )
 * ```
 *
 * ### Custom screen styles:
 * ```
 * AuthenticatorSettingsComponent(
 *     themeConfig = Auth0UiThemeConfig(
 *         screenStyles = ScreenStyles(
 *             otpVerification = OTPVerificationScreenStyle(
 *                 buttonStyle = Auth0ButtonStyle(containerColor = Color.Blue)
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * @param modifier Modifier to be applied to the root container
 * @param themeConfig Theme configuration for customizing the UI appearance.
 *                    Defaults to SDK's default theme if not provided.
 */
@Composable
public fun AuthenticatorSettingsComponent(
    modifier: Modifier = Modifier,
    themeConfig: Auth0UiThemeConfig = Auth0UiThemeConfig()
) {
    Auth0UiTheme(config = themeConfig) {
        val navController = rememberNavController()
        AuthenticatorSettingsNavigationHost(
            navController = navController,
            modifier = modifier
        )
    }
}