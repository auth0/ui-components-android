package com.auth0.universalcomponents.feature.embedded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.auth0.universalcomponents.feature.embedded.presentation.components.StackedEntry

/**
 * How an app changes the entry screen of embedded login and of sign-up: the first screen the user sees, with a logo, a
 * title, the field with Continue, the way to the other flow ("Sign up" or "Log in"), and a button for each other way to
 * sign in. Each part is set on its own, and one left unset keeps its default.
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
 * Build it once, with `remember` or as a constant. It has no `equals`, so a new one on every recomposition of the caller
 * redraws every entry screen that reads it.
 *
 * To change the logo of every entry screen at once, set it in the theme instead (`Auth0ThemeConfiguration.brand`).
 *
 * @property logo Which logo the entry screen shows.
 * @property layout Where the parts of the screen go.
 * @property messages The words of the screen.
 * @property linkColor The color of "Sign up" and "Log in". Null uses the theme's `foregroundLinkPrimary`.
 */
@Immutable
class EmbeddedEntryConfiguration @JvmOverloads constructor(
    val logo: EmbeddedEntryLogo = EmbeddedEntryLogo.Theme,
    val layout: EmbeddedEntryLayout = EmbeddedEntryLayout.Centered,
    val messages: EmbeddedEntryMessages = EmbeddedEntryMessages(),
    val linkColor: Color? = null,
) {
    companion object {

        /** Every part left to its default. One shared instance, so a screen that passes it never looks changed. */
        val Default = EmbeddedEntryConfiguration()
    }
}

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
    /** "Don't have an account? Sign up" or "Already have an account? Log in", when there is one to offer. */
    val accountLink: (@Composable () -> Unit)?,
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
 *         parts.accountLink?.invoke()
 *         parts.alternatives?.invoke()
 *     }
 * }
 * ```
 *
 * The layout is given at least the height of the screen, so a `Column` with `Arrangement.Center` centres its content.
 *
 * The screen scrolls, so the layout is measured with no limit on its height. Don't put a vertical lazy list, such as
 * `LazyColumn`, in it: Compose throws when one is measured inside a vertically scrolling parent.
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
 * The words of the entry screen. One left unset, null or blank, keeps its default, so a title is never an empty heading.
 * The defaults are string resources named `embedded_entry_*`, which an app can also override by declaring a string with
 * the same name.
 *
 * @property loginTitle The login's title above the field when the tenant doesn't offer sign-up. Unset keeps the way's
 * own, such as "Enter your email to get started".
 * @property loginOrSignUpTitle The login's title when the tenant offers sign-up as well. "Log in or sign up".
 * @property signUpTitle The title of sign-up's first screen. "Create your account".
 * @property signUpPrompt The words before the link to sign-up. "Don't have an account?"
 * @property signUpAction The link to sign-up. "Sign up".
 * @property logInPrompt The words before the link back to the login. "Already have an account?"
 * @property logInAction The link back to the login. "Log in".
 */
@Immutable
class EmbeddedEntryMessages @JvmOverloads constructor(
    val loginTitle: String? = null,
    val loginOrSignUpTitle: String? = null,
    val signUpTitle: String? = null,
    val signUpPrompt: String? = null,
    val signUpAction: String? = null,
    val logInPrompt: String? = null,
    val logInAction: String? = null,
)
