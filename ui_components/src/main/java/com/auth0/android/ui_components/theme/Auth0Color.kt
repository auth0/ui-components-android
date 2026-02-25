package com.auth0.android.ui_components.theme

import androidx.compose.ui.graphics.Color


/**
 * Data class representing the Auth0 color scheme with semantic and Material3 bridge colors.
 *
 * This class provides a comprehensive color palette for the Auth0 Android Design System,
 * including both semantic colors for direct use and Material3 bridge colors for compatibility.
 */
data class Auth0Color(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val border: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val foreground: Color,
    val cardForeground: Color,
    val mutedForeground: Color,
) {
    companion object {
        /**
         * Create a light theme color scheme.
         *
         * @return A configured Auth0Color for light theme
         */
        fun light(): Auth0Color = Auth0Color(
            // Primary colors
            primary = Color(0xFF1F1F1F),
            onPrimary = Color(0xFFF0F0F0),
            // Background and surface
            background = Color(0xFFFCFCFC),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF636363),
            // Border
            border = Color(0xFFD9D9D9),
            // Error colors
            error = Color(0xFFF5694D),
            errorContainer = Color(0xFFFFFCFC),
            onError = Color(0xFF5D251D),
            // Success colors
            success = Color(0xFFE6F7EA),
            onSuccess = Color(0xFF1B3D26),
            successContainer = Color(0xFFFAFEFB),
            // Text colors
            textPrimary = Color(0xFF1F1F1F),
            textSecondary = Color(0xFF636363),
            foreground = Color(0xFF1F1F1F),
            cardForeground = Color.Black,
            mutedForeground = Color(0xFF828282)
        )

        /**
         * Create a dark theme color scheme.
         *
         * @return A configured Auth0Color for dark theme
         */
        fun dark(): Auth0Color = Auth0Color(
            // Primary colors
            primary = Color(0xFFEEEEEE),
            onPrimary = Color(0xFF222222),
            // Background and surface
            background = Color(0xFF111111),
            surface = Color(0xFF111111),
            onSurface = Color(0xFFB4B4B4),
            // Border
            border = Color(0xFF3A3A3A),
            // Error colors
            error = Color(0xFF400D07),
            errorContainer = Color(0xFF180E0D),
            onError = Color(0xFFFFCFC5),
            // Success colors
            success = Color(0xFF152D1C),
            onSuccess = Color(0xFFB1F2C2),
            successContainer = Color(0xFF0C130E),
            // Text colors
            textPrimary = Color(0xFFEEEEEE),
            textSecondary = Color(0xFFB4B4B4),
            foreground = Color(0xFFEEEEEE),
            cardForeground = Color.White,
            mutedForeground = Color(0xFF7B7B7B)
        )
    }
}
