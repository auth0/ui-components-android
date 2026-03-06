package com.auth0.android.ui_components.theme

import androidx.compose.ui.graphics.Color


/**
 * Data class representing the Auth0 color scheme with semantic and Material3 bridge colors.
 *
 * This class provides a comprehensive color palette for the Auth0 Android Design System,
 * including both semantic colors for direct use and Material3 bridge colors for compatibility.
 */
data class Auth0Color(
    // Background

    /** Default background color for CTA. */
    val backgroundPrimary: Color,
    /** Softer variant of the primary background for low-emphasis areas. */
    val backgroundPrimarySubtle: Color,
    /** Background that flips contrast, used against the primary background. */
    val backgroundInverse: Color,
    /** Topmost layer background, for overlays, modals, and popovers. */
    val backgroundLayerTop: Color,
    /** Mid-level layer background, for cards and raised containers. */
    val backgroundLayerMedium: Color,
    /** Foundational layer background, sitting beneath all other layers. */
    val backgroundLayerBase: Color,
    /** Background for error states, alerts, and destructive messaging. */
    val backgroundError: Color,
    /** Muted error background for low-severity or inline error hints. */
    val backgroundErrorSubtle: Color,
    /** Background for success states and positive confirmations. */
    val backgroundSuccess: Color,
    /** Muted success background for subtle positive feedback. */
    val backgroundSuccessSubtle: Color,
    /** Background used to highlight branded or featured UI elements. */
    val backgroundAccent: Color,

    // Border

    /** High-contrast border for emphasis or strong visual separation and selected elements. */
    val borderBold: Color,
    /** Standard border color for most UI elements and containers. */
    val borderDefault: Color,
    /** Low-contrast border for delicate dividers and understated boundaries. */
    val borderSubtle: Color,
    /** Border-like shadow color for depth and elevation cues. */
    val borderShadow: Color,

    // Text

    /** High-emphasis color for default body text on neutral backgrounds or surfaces. */
    val textBold: Color,
    /** Lower-emphasis color for helper text, captions, or secondary information. */
    val textDefault: Color,
    /** Color used for disabled text. */
    val textDisabled: Color,
    /** Color for text and icons placed on top of a primary background, ensuring readable contrast. */
    val textOnPrimary: Color,
    /** Color for text and icons placed on top of a success background, ensuring readable contrast. */
    val textOnSuccess: Color,
    /** Color for text and icons placed on top of an error background, ensuring readable contrast. */
    val textOnError: Color,
) {
    companion object {
        /**
         * Create a light theme color scheme.
         *
         * @return A configured Auth0Color for light theme
         */
        fun light(): Auth0Color = Auth0Color(
            // Background colors
            backgroundPrimary = Color(0xFF09090B),
            backgroundPrimarySubtle = Color(0x5909090B), // #09090B at 35% alpha
            backgroundInverse = Color(0xFF18181B),
            backgroundLayerTop = Color(0xFFFFFFFF),
            backgroundLayerMedium = Color(0xFFFCFCFC),
            backgroundLayerBase = Color(0xFFF4F4F5),
            backgroundError = Color(0xFFFDECE8),
            backgroundErrorSubtle = Color(0xFFFFFCFC),
            backgroundSuccess = Color(0xFFE6F7EA),
            backgroundSuccessSubtle = Color(0xFFFAFEFB),
            backgroundAccent = Color(0xFF09090B),
            // Border colors
            borderBold = Color(0xFFA1A1AA),
            borderDefault = Color(0xFFD9D9D9),
            borderSubtle = Color(0xFFE4E4E7),
            borderShadow = Color(0x80CECECE), // #CECECE at 50% alpha
            // Text colors
            textBold = Color(0xFF1F1F1F),
            textDefault = Color(0xFF636363),
            textDisabled = Color(0xFF8E8E8E),
            textOnPrimary = Color(0xFFF0F0F0),
            textOnSuccess = Color(0xFF6EE7B7),
            textOnError = Color(0xFF5D251D),
        )

        /**
         * Create a dark theme color scheme.
         *
         * @return A configured Auth0Color for dark theme
         */
        fun dark(): Auth0Color = Auth0Color(
            // Background colors
            backgroundPrimary = Color(0xFFFAFAFA),
            backgroundPrimarySubtle = Color(0x80FAFAFA), // #FAFAFA at 50% alpha
            backgroundInverse = Color(0xFFFAFAFA),
            backgroundLayerTop = Color(0xFF3F3F46),
            backgroundLayerMedium = Color(0xFF27272A),
            backgroundLayerBase = Color(0xFF09090B),
            backgroundError = Color(0xFFFDA4AF),
            backgroundErrorSubtle = Color(0xFFBE123C),
            backgroundSuccess = Color(0xFFA7F3D0),
            backgroundSuccessSubtle = Color(0xFF059669),
            backgroundAccent = Color(0xFFA7F3D0),
            // Border colors
            borderBold = Color(0xFF71717A),
            borderDefault = Color(0xFF3F3F46),
            borderSubtle = Color(0x00D4D4D4), // #D4D4D4 at 0% alpha
            borderShadow = Color(0xFF3F3F46),
            // Text colors
            textBold = Color(0xFFFAFAFA),
            textDefault = Color(0xFFA1A1AA),
            textDisabled = Color(0xFF52525B),
            textOnPrimary = Color(0xFF18181B),
            textOnSuccess = Color(0xFFA7F3D0),
            textOnError = Color(0xFFFFCFC5),
        )
    }
}
