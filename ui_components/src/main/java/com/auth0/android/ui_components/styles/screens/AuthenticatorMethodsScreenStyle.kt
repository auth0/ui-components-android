package com.auth0.android.ui_components.styles.screens

import android.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.auth0.android.ui_components.styles.components.AuthenticatorItemStyle
import com.auth0.android.ui_components.styles.components.TopBarStyle
import com.auth0.android.ui_components.theme.Auth0ColorScheme
import com.auth0.android.ui_components.theme.Auth0Dimensions
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Style configuration for the Authenticator Methods screen.
 *
 * This screen displays the list of available authentication methods (TOTP, Email, Phone, etc.).
 * All properties are optional. When null, the screen will use values from the theme.
 *
 * Example:
 * ```
 * AuthenticatorMethodsScreenStyle(
 *     backgroundColor = Color(0xFFF5F5F5),
 *     topBarStyle = TopBarStyle(
 *         backgroundColor = Color.White,
 *         showDivider = true
 *     ),
 *     sectionHeaderStyle = TextStyle(
 *         fontWeight = FontWeight.Bold,
 *         fontSize = 22.sp
 *     )
 * )
 * ```
 */
@Immutable
public data class AuthenticatorMethodsScreenStyle(
    val backgroundColor: Color = Auth0Theme.colors.background,
    val topBarStyle: TopBarStyle = TopBarStyle(),
    val listItemStyle: AuthenticatorItemStyle = AuthenticatorItemStyle.Default,
    val horizontalPadding: Dp = Auth0Theme.dimensions.horizontalPadding,
    val verticalPadding: Dp = Auth0Theme.dimensions.verticalPadding,
    val itemSpacing: Dp = Auth0Theme.dimensions.spacingMd
)
