package com.auth0.universalcomponents.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.auth0.universalcomponents.R

/**
 * The brand's own assets: what an Auth0 screen shows to say whose it is. The defaults are Auth0's; an app replaces them
 * once, in [Auth0ThemeConfiguration.brand], and every screen that shows a logo follows.
 *
 * ```
 * Auth0ThemeConfiguration(
 *     brand = Auth0Brand(logo = R.drawable.acme_logo, logoDescription = "Acme"),
 * )
 * ```
 *
 * @property logo The logo shown above the title of the embedded login entry screens. Auth0's by default.
 * @property logoDescription What a screen reader says for the logo. "Auth0" by default.
 * @property logoTint How the logo is coloured. `null`, the default, draws it in the theme's text color, which keeps a
 * one-color logo, like Auth0's, readable in dark mode. [Color.Unspecified] draws it as it is, for a logo with its own
 * colors. Any other color tints it.
 */
@Immutable
data class Auth0Brand(
    @param:DrawableRes val logo: Int = R.drawable.auth0_logo,
    val logoDescription: String = "Auth0",
    val logoTint: Color? = null,
)
