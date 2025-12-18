package com.auth0.android.ui_components.theme

import androidx.compose.runtime.Immutable
import com.auth0.android.ui_components.styles.screens.AuthenticatorMethodsScreenStyle

/**
 * Main configuration class for Auth0 UI theme.
 *
 * This is the primary entry point for customizing the appearance of Auth0 UI components.
 *
 * All parameters have sensible defaults, so you only need to specify what you want to customize.
 *
 * ## Usage Examples
 *
 * ### Use all defaults (no customization needed):
 * ```
 * AuthenticatorSettingsComponent()
 * ```
 *
 * ### Customize colors only:
 * ```
 * AuthenticatorSettingsComponent(
 *     themeConfig = Auth0UiThemeConfig(
 *         colors = Auth0ColorScheme(
 *             primary = Color(0xFF6200EE),
 *             onPrimary = Color.White
 *         )
 *     )
 * )
 * ```
 *
 * ### Customize a specific screen:
 * ```
 * AuthenticatorSettingsComponent(
 *     themeConfig = Auth0UiThemeConfig(
 *         screenStyles = ScreenStyles(
 *             otpVerification = OTPVerificationScreenStyle(
 *                 buttonStyle = Auth0ButtonStyle(
 *                     containerColor = Color(0xFF6200EE)
 *                 )
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * ### Full brand customization:
 * ```
 * val myBrandTheme = Auth0UiThemeConfig(
 *     colors = Auth0ColorScheme(
 *         primary = MyBrandColors.Primary,
 *         background = MyBrandColors.Background
 *     ),
 *     typography = Auth0Typography(
 *         titleLarge = MyBrandTypography.TitleStyle
 *     ),
 *     shapes = Auth0Shapes(
 *         large = RoundedCornerShape(20.dp)
 *     )
 * )
 *
 * AuthenticatorSettingsComponent(themeConfig = myBrandTheme)
 * ```
 *
 * @param colors Color scheme for the UI components
 * @param typography Typography scheme for text styles
 * @param shapes Shape scheme for corners and borders
 * @param dimensions Dimension tokens for spacing and sizing
 * @param screenStyles Per-screen style customizations
 */
@Immutable
public data class Auth0UiThemeConfig(
    val colors: Auth0ColorScheme = Auth0ColorScheme(),
    val typography: Auth0Typography = Auth0Typography(),
    val shapes: Auth0Shapes = Auth0Shapes(),
    val dimensions: Auth0Dimensions = Auth0Dimensions(),
    val screenStyles: ScreenStyles = ScreenStyles()
)

/**
 * Container for screen-level style customizations.
 *
 * Each screen in the SDK has its own style class that allows fine-grained customization.
 *
 * Example:
 * ```
 * ScreenStyles(
 *     authenticatorMethods = AuthenticatorMethodsScreenStyle(
 *         backgroundColor = Color(0xFFF5F5F5)
 *     )
 * )
 * ```
 */
@Immutable
public data class ScreenStyles(
    /** Style for the Authenticator Methods screen */
    val authenticatorMethods: AuthenticatorMethodsScreenStyle = AuthenticatorMethodsScreenStyle(),
)
