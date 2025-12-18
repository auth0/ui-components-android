package com.auth0.android.ui_components.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for providing Auth0 theme throughout the SDK.
 */
internal val LocalAuth0Theme = staticCompositionLocalOf { Auth0UiThemeConfig() }

/**
 * Internal theme provider composable.
 * Wraps content with the Auth0 theme configuration.
 */
@Composable
internal fun Auth0UiTheme(
    config: Auth0UiThemeConfig = Auth0UiThemeConfig(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAuth0Theme provides config
    ) {
        content()
    }
}

/**
 * Accessor object for retrieving current theme values within SDK components.
 *
 * This is internal and used by SDK components to access theme values.
 * SDK consumers should use [Auth0UiThemeConfig] to customize the theme.
 *
 * Example usage within SDK:
 * ```
 * @Composable
 * fun MyComponent() {
 *     val backgroundColor = Auth0Theme.colors.background
 *     val titleStyle = Auth0Theme.typography.titleLarge
 *     // ...
 * }
 * ```
 */
internal object Auth0Theme {
    /**
     * Current color scheme.
     */
    val colors: Auth0ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAuth0Theme.current.colors

    /**
     * Current typography scheme.
     */
    val typography: Auth0Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalAuth0Theme.current.typography

    /**
     * Current shape scheme.
     */
    val shapes: Auth0Shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAuth0Theme.current.shapes

    /**
     * Current dimension tokens.
     */
    val dimensions: Auth0Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAuth0Theme.current.dimensions

    /**
     * Current screen styles.
     */
    val screenStyles: ScreenStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalAuth0Theme.current.screenStyles
}
