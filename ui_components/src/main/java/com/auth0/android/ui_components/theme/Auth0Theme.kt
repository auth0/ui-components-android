package com.auth0.android.ui_components.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable

/**
 * Configuration object for Auth0Theme.
 *
 * Allows customization of theme tokens. Any null value will use the default
 * value based on dark mode settings.
 *
 * @property color Custom colors, or null to use default for dark mode
 * @property typography Custom typography scale, or null to use default
 * @property shapes Custom shapes, or null to use default
 * @property dimensions Custom dimensions/spacing, or null to use default
 */
@Immutable
data class Auth0ThemeConfiguration(
    val color: Auth0Color? = null,
    val typography: Auth0Typography? = null,
    val shapes: Auth0Shapes? = null,
    val dimensions: Auth0Dimensions? = null
) {
    companion object {
        /**
         * Default theme configuration.
         * Uses system-appropriate light/dark colors with default tokens.
         */
        val Default = Auth0ThemeConfiguration()
    }
}

/**
 * Auth0 theme provider composable.
 *
 * Wraps content with Auth0 design tokens and Material3 theme.
 * All Auth0 UI components must be descendants of this composable.
 *
 * Usage:
 * ```kotlin
 * // Default theme
 * Auth0Theme {
 *     MyContent()
 * }
 *
 * // Custom brand colors
 * Auth0Theme(
 *     configuration = Auth0ThemeConfiguration(
 *         color = Auth0Color.light().copy(
 *             primary = Color(0xFFFF6B00)
 *         )
 *     )
 * ) {
 *     MyContent()
 * }
 *
 * // Force dark mode
 * Auth0Theme(darkTheme = true) {
 *     MyContent()
 * }
 * ```
 *
 * @param configuration Theme configuration with optional custom tokens
 * @param darkTheme Force dark mode (true), light mode (false), or system default (null)
 * @param content The composable content to wrap with the theme
 */
@Composable
fun Auth0Theme(
    configuration: Auth0ThemeConfiguration = Auth0ThemeConfiguration.Default,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // Step 1: Resolve dark mode
    val isDark = darkTheme ?: isSystemInDarkTheme()

    // Step 2: Resolve each token type (custom override or default)
    val color = configuration.color ?: if (isDark) {
        Auth0Color.dark()
    } else {
        Auth0Color.light()
    }

    val typography = configuration.typography ?: Auth0Typography.default()

    val shapes = configuration.shapes ?: Auth0Shapes.default()

    val dimensions = configuration.dimensions ?: Auth0Dimensions.default()

    // Step 3: Bridge Auth0 tokens to Material3
    val material3ColorScheme = color.toMaterial3ColorScheme(isDark)
    val material3Typography = typography.toMaterial3Typography()
    val material3Shapes = shapes.toMaterial3Shapes()

    // Step 4: Provide all CompositionLocals
    CompositionLocalProvider(
        LocalAuth0Color provides color,
        LocalAuth0Typography provides typography,
        LocalAuth0Shapes provides shapes,
        LocalAuth0Dimensions provides dimensions
    ) {
        // Step 5: Wrap with MaterialTheme
        MaterialTheme(
            colorScheme = material3ColorScheme,
            typography = material3Typography,
            shapes = material3Shapes,
            content = content
        )
    }
}

/**
 * Bridges Auth0Color to Material3 ColorScheme.
 * Maps Auth0 semantic tokens to Material3 slots.
 */
private fun Auth0Color.toMaterial3ColorScheme(isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = this.primary,
            onPrimary = this.onPrimary,
            background = this.background,
            surface = this.surface,
            onSurface = this.onSurface,
            error = this.error,
            errorContainer = this.errorContainer,
            onError = this.onError,
            // Map success to tertiary (Material3 doesn't have semantic "success")
            tertiary = this.success,
            onTertiary = this.onSuccess,
            tertiaryContainer = this.successContainer,
            // Use border for outline
            outline = this.border,
            // Use textSecondary for onSurfaceVariant
            onSurfaceVariant = this.textSecondary
        )
    } else {
        lightColorScheme(
            primary = this.primary,
            onPrimary = this.onPrimary,
            background = this.background,
            surface = this.surface,
            onSurface = this.onSurface,
            error = this.error,
            errorContainer = this.errorContainer,
            onError = this.onError,
            tertiary = this.success,
            onTertiary = this.onSuccess,
            tertiaryContainer = this.successContainer,
            outline = this.border,
            onSurfaceVariant = this.textSecondary
        )
    }
}

/**
 * Bridges Auth0Typography to Material3 Typography.
 * Maps Auth0 type scale to Material3 slots.
 */
private fun Auth0Typography.toMaterial3Typography(): Typography {
    return Typography(
        displayLarge = this.displayLarge,
        displayMedium = this.displayMedium,
        displaySmall = this.display,  // Auth0's "display" -> Material3's displaySmall
        headlineLarge = this.titleLarge,
        headlineMedium = this.title,
        bodyLarge = this.bodyLarge,
        bodyMedium = this.bodyMedium,
        bodySmall = this.bodySmall,
        labelLarge = this.labelLarge,
        labelMedium = this.labelMedium,
        labelSmall = this.labelSmall
    )
}

/**
 * Bridges Auth0Shapes to Material3 Shapes.
 * Maps Auth0 shape tokens to Material3 slots.
 */
private fun Auth0Shapes.toMaterial3Shapes(): Shapes {
    return Shapes(
        extraSmall = this.extraSmall,
        small = this.small,
        medium = this.medium,
        large = this.large,
        extraLarge = this.extraLarge
    )
}
