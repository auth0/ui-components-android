package com.auth0.universalcomponents.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.auth0.universalcomponents.R

/**
 * The brand's own assets: what an Auth0 screen shows to say whose it is. The defaults are Auth0's; an app replaces them
 * once, in [Auth0ThemeConfiguration.brand], and the entry screens follow.
 *
 * ```
 * Auth0ThemeConfiguration(
 *     brand = Auth0Brand(logo = R.drawable.acme_logo, logoDescription = "Acme"),
 * )
 * ```
 *
 * @property logo The logo shown above the title of the embedded login entry screens. Auth0's by default. A vector or
 * bitmap drawable; an XML shape or layer list can't be drawn here, so show one with `EmbeddedEntryLogo.Custom { }`.
 * @property logoDescription What a screen reader says for the logo. `null`, the default, says "Auth0" for Auth0's own
 * logo and nothing for your [logo], which a screen reader then skips; set it to describe your logo.
 * @property logoTint How the logo is coloured. `null`, the default, leaves it to the library: Auth0's own logo is drawn
 * in the theme's text color, which keeps its one color readable in dark mode, and your [logo] is drawn as it is.
 * [Color.Unspecified] draws any logo as it is, and any other color tints it.
 */
@Immutable
data class Auth0Brand(
    @param:DrawableRes val logo: Int = R.drawable.uc_auth0_logo,
    val logoDescription: String? = null,
    val logoTint: Color? = null,
)

/**
 * What a screen reader says for the logo. What the app set wins; left to the library ([Auth0Brand.logoDescription] is
 * `null`), only Auth0's own logo is named, so an app's logo is never announced as "Auth0".
 */
internal fun Auth0Brand.descriptionOrDefault(): String? = logoDescription
    ?: if (logo == R.drawable.uc_auth0_logo) AUTH0_LOGO_DESCRIPTION else null

private const val AUTH0_LOGO_DESCRIPTION = "Auth0"

/**
 * The color to tint the logo with. What the app set wins; left to the library ([Auth0Brand.logoTint] is `null`), only
 * Auth0's own logo, which is one color and has to follow the theme, is tinted, in [textColor]. The app's is not touched:
 * a logo in several colors would turn into one, and a bitmap into a solid block.
 */
internal fun Auth0Brand.tintOr(textColor: Color): Color = logoTint
    ?: if (logo == R.drawable.uc_auth0_logo) textColor else Color.Unspecified
