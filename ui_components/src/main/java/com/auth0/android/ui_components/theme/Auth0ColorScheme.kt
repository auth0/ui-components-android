package com.auth0.android.ui_components.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color scheme for Auth0 UI components.
 *
 * Provides a consistent color palette across all SDK screens and components.
 * All parameters have default values, so you only need to specify what you want to customize.
 *
 * Example:
 * ```
 * // Customize only the primary color
 * Auth0ColorScheme(primary = Color(0xFF6200EE))
 *
 * // Customize multiple colors
 * Auth0ColorScheme(
 *     primary = Color(0xFF6200EE),
 *     onPrimary = Color.White,
 *     background = Color(0xFFF5F5F5)
 * )
 * ```
 */
@Immutable
public data class Auth0ColorScheme(

    val primary: Color = Color(0xFF262420),          // ButtonBlack

    val onPrimary: Color = Color.White,

    val background: Color = Color.White,

    val surface: Color = Color.White,

    val onSurface: Color = Color(0xFF1D1B20),        // TopBarTitle

    val error: Color = Color(0xFFB82819),            // ErrorRed

    val onError: Color = Color.White,

    val success: Color = Color(0xFF149750),          // ActiveLabelText

    val successContainer: Color = Color(0xFFE5F4EA), // ActiveLabelBackground

    val textPrimary: Color = Color.Black,

    val textSecondary: Color = Color(0xFF606060),    // secondaryTextColor

    val border: Color = Color(0xFFD9D9D9),           // AuthenticatorItemBorder

)
