package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.auth0.universalcomponents.feature.embedded.EmbeddedEntryConfiguration
import com.auth0.universalcomponents.feature.embedded.EmbeddedEntryLogo

// What the entry screen needs behind the public [EmbeddedEntryConfiguration], kept apart from it so that file holds
// only what an app can use.

/**
 * What the entry screen is configured with, for the screens below the login and the sign-up. Not a static local: a new
 * value then refreshes the screens that read it, and not everything below them.
 */
internal val LocalEmbeddedEntry = compositionLocalOf { EmbeddedEntryConfiguration.Default }

/** The logo to draw: the one the theme has when [Theme][EmbeddedEntryLogo.Theme], the app's own, or none. */
internal fun EmbeddedEntryLogo.resolve(theme: @Composable () -> Unit): (@Composable () -> Unit)? = when (this) {
    EmbeddedEntryLogo.Theme -> theme
    is EmbeddedEntryLogo.Custom -> content
    EmbeddedEntryLogo.Hidden -> null
}
