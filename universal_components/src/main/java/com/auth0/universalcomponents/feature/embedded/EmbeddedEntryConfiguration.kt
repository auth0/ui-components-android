package com.auth0.universalcomponents.feature.embedded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.auth0.universalcomponents.feature.embedded.presentation.components.StackedEntry

/**
 * How an app changes the entry screen of embedded login: the first screen the user sees, with a logo, a title, the field
 * with Continue, and a button for each other way to sign in. Each part is set on its own, and one left unset keeps its
 * default.
 *
 * ```
 * EmbeddedLoginScreen(
 *     …,
 *     entry = EmbeddedEntryConfiguration(
 *         logo = EmbeddedEntryLogo.Hidden,
 *         layout = EmbeddedEntryLayout.Top,
 *         messages = EmbeddedEntryMessages(loginTitle = "Welcome back"),
 *     ),
 * )
 * ```
 *
 * To change the logo everywhere at once, set it in the theme instead (`Auth0ThemeConfiguration.brand`).
 *
 * @property logo Which logo the entry screen shows.
 * @property layout Where the parts of the screen go.
 * @property messages The words of the screen.
 */
@Immutable
class EmbeddedEntryConfiguration @JvmOverloads constructor(
    val logo: EmbeddedEntryLogo = EmbeddedEntryLogo.Theme,
    val layout: EmbeddedEntryLayout = EmbeddedEntryLayout.Centered,
    val messages: EmbeddedEntryMessages = EmbeddedEntryMessages(),
)

/** Which logo the entry screen shows. */
sealed interface EmbeddedEntryLogo {

    /** The theme's: `Auth0ThemeConfiguration.brand`, Auth0's unless the app's theme replaces it. The default. */
    data object Theme : EmbeddedEntryLogo

    /**
     * A logo of the app's own, for this screen only. It is drawn as it is: the size, color and screen reader
     * description are the app's to set.
     */
    class Custom(val content: @Composable () -> Unit) : EmbeddedEntryLogo

    /** No logo. */
    data object Hidden : EmbeddedEntryLogo
}

/**
 * The pieces of an entry screen, built by the library and placed by an [EmbeddedEntryLayout]. A layout may leave a piece
 * out, move it or wrap it, but the field and Continue stay together in [form]. A piece that isn't there is null.
 *
 * A later release may add pieces, so a layout only places the ones it knows; what it doesn't place isn't drawn.
 */
class EmbeddedEntryParts internal constructor(
    /** The logo: the theme's by default, or what the app set in [EmbeddedEntryConfiguration]. Null when hidden. */
    val logo: (@Composable () -> Unit)?,
    val title: @Composable () -> Unit,
    /** The field and Continue. */
    val form: @Composable () -> Unit,
    /** "OR" and a button for each other way, when the tenant offers more than one. */
    val alternatives: (@Composable () -> Unit)?,
)

/**
 * Where the pieces of the entry screen go. It is drawn inside the screen's usual padding and scrolls, so a layout only
 * arranges the pieces. [Centered] is the default; an app can write its own:
 *
 * ```
 * EmbeddedEntryLayout { parts ->
 *     Column {
 *         parts.logo?.invoke()
 *         parts.title()
 *         parts.form()
 *         parts.alternatives?.invoke()
 *     }
 * }
 * ```
 *
 * The layout is given at least the height of the screen, so a `Column` with `Arrangement.Center` centres its content.
 */
class EmbeddedEntryLayout(val content: @Composable (EmbeddedEntryParts) -> Unit) {

    companion object {

        /** The logo, title, field and buttons as one block in the middle of the screen. The default. */
        val Centered = EmbeddedEntryLayout { StackedEntry(it, isCentered = true) }

        /** The same block, starting at the top of the screen. */
        val Top = EmbeddedEntryLayout { StackedEntry(it, isCentered = false) }
    }
}

/**
 * The words of the entry screen. One left unset keeps its default.
 *
 * @property loginTitle The title above the field. Null keeps the way's own, such as "Enter your email to get started".
 */
@Immutable
class EmbeddedEntryMessages @JvmOverloads constructor(
    val loginTitle: String? = null,
)

/** What the entry screen is configured with, for the screens below [EmbeddedLoginScreen]. */
internal val LocalEmbeddedEntry = staticCompositionLocalOf { EmbeddedEntryConfiguration() }

/** The logo to draw: the one the theme has when [Theme][EmbeddedEntryLogo.Theme], the app's own, or none. */
internal fun EmbeddedEntryLogo.resolve(theme: @Composable () -> Unit): (@Composable () -> Unit)? = when (this) {
    EmbeddedEntryLogo.Theme -> theme
    is EmbeddedEntryLogo.Custom -> content
    EmbeddedEntryLogo.Hidden -> null
}
