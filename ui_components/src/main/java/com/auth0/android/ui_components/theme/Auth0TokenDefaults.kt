package com.auth0.android.ui_components.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Public accessor object for Auth0 design tokens.
 * All components should use this object to read tokens from the theme.
 *
 * Must be called from within an Auth0Theme composable.
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun MyButton() {
 *     val colors = Auth0TokenDefaults.color()
 *     val typography = Auth0TokenDefaults.typography()
 *
 *     Button(
 *         colors = ButtonDefaults.buttonColors(
 *             containerColor = colors.primary
 *         )
 *     ) {
 *         Text("Click me", style = typography.labelLarge)
 *     }
 * }
 * ```
 */
object Auth0TokenDefaults {

    /**
     * Returns the current Auth0 colors (light or dark mode).
     *
     * @throws IllegalStateException if called outside Auth0Theme
     */
    @Composable
    @ReadOnlyComposable
    fun color(): Auth0Color = LocalAuth0Color.current

    /**
     * Returns the current Auth0 typography scale.
     *
     * @throws IllegalStateException if called outside Auth0Theme
     */
    @Composable
    @ReadOnlyComposable
    fun typography(): Auth0Typography = LocalAuth0Typography.current

    /**
     * Returns the current Auth0 shapes (corner radii).
     *
     * @throws IllegalStateException if called outside Auth0Theme
     */
    @Composable
    @ReadOnlyComposable
    fun shapes(): Auth0Shapes = LocalAuth0Shapes.current

    /**
     * Returns the current Auth0 dimensions (spacing values).
     *
     * @throws IllegalStateException if called outside Auth0Theme
     */
    @Composable
    @ReadOnlyComposable
    fun dimensions(): Auth0Dimensions = LocalAuth0Dimensions.current
}
