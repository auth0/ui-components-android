package com.auth0.android.sample.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.Auth0Color
import com.auth0.android.ui_components.theme.Auth0Shapes
import com.auth0.android.ui_components.theme.Auth0ThemeConfiguration

/**
 * Pre-built theme presets demonstrating Auth0 token system customization capabilities.
 *
 * Each preset showcases a different aspect of the theming API:
 * - Presets 1-2: Built-in light/dark color schemes
 * - Presets 3-4: Custom brand color overrides
 * - Presets 5-6: Shape customization
 */
sealed class ThemePreset(
    val name: String,
    val description: String,
    val configuration: Auth0ThemeConfiguration,
    val darkTheme: Boolean?,
    val codeSnippet: String
) {
    /** Standard Auth0 light theme using default token values. */
    data object DefaultLight : ThemePreset(
        name = "Default Light",
        description = "Standard Auth0 light theme with default tokens.",
        configuration = Auth0ThemeConfiguration(
            color = Auth0Color.light()
        ),
        darkTheme = false,
        codeSnippet = """
AuthenticatorSettingsComponent(
    themeConfiguration = Auth0ThemeConfiguration(
        color = Auth0Color.light()
    )
)""".trimIndent()
    )

    /** Standard Auth0 dark theme using built-in dark color scheme. */
    data object DefaultDark : ThemePreset(
        name = "Default Dark",
        description = "Standard Auth0 dark theme with dark color scheme.",
        configuration = Auth0ThemeConfiguration(
            color = Auth0Color.dark()
        ),
        darkTheme = true,
        codeSnippet = """
AuthenticatorSettingsComponent(
    themeConfiguration = Auth0ThemeConfiguration(
        color = Auth0Color.dark()
    )
)""".trimIndent()
    )

    /** Custom orange brand color applied to the primary token. */
    data object OrangeBrand : ThemePreset(
        name = "Orange Brand",
        description = "Custom primary color: vibrant orange for brand identity.",
        configuration = Auth0ThemeConfiguration(
            color = Auth0Color.light().copy(
                backgroundPrimary = Color(0xFFFF6B00),
                textOnPrimary = Color.White
            )
        ),
        darkTheme = false,
        codeSnippet = """
AuthenticatorSettingsComponent(
    themeConfiguration = Auth0ThemeConfiguration(
        color = Auth0Color.light().copy(
            backgroundPrimary = Color(0xFFFF6B00),
            textOnPrimary = Color.White
        )
    )
)""".trimIndent()
    )

    /** Custom teal brand color applied to the primary token. */
    data object TealBrand : ThemePreset(
        name = "Teal Brand",
        description = "Custom primary color: fresh teal for brand identity.",
        configuration = Auth0ThemeConfiguration(
            color = Auth0Color.light().copy(
                backgroundPrimary = Color(0xFF00BFA5),
                textOnPrimary = Color.White
            )
        ),
        darkTheme = false,
        codeSnippet = """
AuthenticatorSettingsComponent(
    themeConfiguration = Auth0ThemeConfiguration(
        color = Auth0Color.light().copy(
            backgroundPrimary = Color(0xFF00BFA5),
            textOnPrimary = Color.White
        )
    )
)""".trimIndent()
    )

    /** Compact shapes with minimal corner radius for a modern flat design. */
    data object CompactShapes : ThemePreset(
        name = "Compact Shapes",
        description = "Minimal corner radius (4dp/8dp) for modern flat design.",
        configuration = Auth0ThemeConfiguration(
            shapes = Auth0Shapes(
                none = RoundedCornerShape(0.dp),
                extraSmall = RoundedCornerShape(2.dp),
                small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(6.dp),
                large = RoundedCornerShape(8.dp),
                extraLarge = RoundedCornerShape(10.dp),
                full = RoundedCornerShape(100.dp)
            )
        ),
        darkTheme = false,
        codeSnippet = """
AuthenticatorSettingsComponent(
    themeConfiguration = Auth0ThemeConfiguration(
        shapes = Auth0Shapes(
            none = RoundedCornerShape(0.dp),
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(10.dp),
            full = RoundedCornerShape(100.dp)
        )
    )
)""".trimIndent()
    )

    /** Bold shapes with large corner radius for a friendly rounded design. */
    data object BoldShapes : ThemePreset(
        name = "Bold Shapes",
        description = "Large corner radius (24dp) for friendly rounded design.",
        configuration = Auth0ThemeConfiguration(
            shapes = Auth0Shapes(
                none = RoundedCornerShape(0.dp),
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(32.dp),
                full = RoundedCornerShape(100.dp)
            )
        ),
        darkTheme = false,
        codeSnippet = """
AuthenticatorSettingsComponent(
    themeConfiguration = Auth0ThemeConfiguration(
        shapes = Auth0Shapes(
            none = RoundedCornerShape(0.dp),
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp),
            full = RoundedCornerShape(100.dp)
        )
    )
)""".trimIndent()
    )

    // /** Custom olive/dark green brand color matching the Figma "Custom Theme (Olive)". */
    // data object OliveBrand : ThemePreset(
    //     name = "Custom Theme (Olive)",
    //     description = "Dark olive green primary for an earthy, professional look.",
    //     configuration = Auth0ThemeConfiguration(
    //         color = Auth0Color.light().copy(
    //             backgroundPrimary = Color(0xFF3D4F27),
    //             textOnPrimary = Color.White,
    //             backgroundLayerBase = Color(0xFFF8F6F0)
    //         )
    //     ),
    //     darkTheme = false,
    //     codeSnippet = """
    // AuthenticatorSettingsComponent(
    //     themeConfiguration = Auth0ThemeConfiguration(
    //         color = Auth0Color.light().copy(
    //             backgroundPrimary = Color(0xFF3D4F27),
    //             textOnPrimary = Color.White,
    //             backgroundLayerBase = Color(0xFFF8F6F0)
    //         )
    //     )
    // )""".trimIndent()
    // )

    // /** Custom purple brand color matching the Figma "Custom Theme (Purple)". */
    // data object PurpleBrand : ThemePreset(
    //     name = "Custom Theme (Purple)",
    //     description = "Vibrant purple primary for a bold, modern feel.",
    //     configuration = Auth0ThemeConfiguration(
    //         color = Auth0Color.light().copy(
    //             backgroundPrimary = Color(0xFF6B21A8),
    //             textOnPrimary = Color.White
    //         )
    //     ),
    //     darkTheme = false,
    //     codeSnippet = """
    // AuthenticatorSettingsComponent(
    //     themeConfiguration = Auth0ThemeConfiguration(
    //         color = Auth0Color.light().copy(
    //             backgroundPrimary = Color(0xFF6B21A8),
    //             textOnPrimary = Color.White
    //         )
    //     )
    // )""".trimIndent()
    // )

    companion object {
        fun all(): List<ThemePreset> = listOf(
            DefaultLight,
            DefaultDark,
            OrangeBrand,
            TealBrand,
            CompactShapes,
            BoldShapes,
            // OliveBrand,
            // PurpleBrand
        )
    }
}
