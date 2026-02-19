package com.auth0.android.ui_components.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for Auth0 color tokens.
 * Must be provided by Auth0Theme, will throw error if accessed outside Auth0Theme.
 */
internal val LocalAuth0Color = staticCompositionLocalOf<Auth0Color> {
    error("No Auth0Color provided. Wrap your composable with Auth0Theme { }.")
}

/**
 * CompositionLocal for Auth0 typography tokens.
 * Must be provided by Auth0Theme, will throw error if accessed outside Auth0Theme.
 */
internal val LocalAuth0Typography = staticCompositionLocalOf<Auth0Typography> {
    error("No Auth0Typography provided. Wrap your composable with Auth0Theme { }.")
}

/**
 * CompositionLocal for Auth0 shape tokens.
 * Must be provided by Auth0Theme, will throw error if accessed outside Auth0Theme.
 */
internal val LocalAuth0Shapes = staticCompositionLocalOf<Auth0Shapes> {
    error("No Auth0Shapes provided. Wrap your composable with Auth0Theme { }.")
}

/**
 * CompositionLocal for Auth0 dimension/spacing tokens.
 * Must be provided by Auth0Theme, will throw error if accessed outside Auth0Theme.
 */
internal val LocalAuth0Dimensions = staticCompositionLocalOf<Auth0Dimensions> {
    error("No Auth0Dimensions provided. Wrap your composable with Auth0Theme { }.")
}
