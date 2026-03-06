package com.auth0.android.ui_components.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Single CompositionLocal for all Auth0 theme tokens.
 * Must be provided by Auth0Theme, will throw error if accessed outside Auth0Theme.
 *
 */
internal val LocalAuth0Theme = staticCompositionLocalOf<Auth0Theme.Values> {
    error("No Auth0Theme provided. Wrap your composable with Auth0Theme { }.")
}
