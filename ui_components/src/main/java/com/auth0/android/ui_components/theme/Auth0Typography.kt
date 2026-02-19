package com.auth0.android.ui_components.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Auth0 Typography data class containing all typography styles for the design system.
 * No colors are embedded in TextStyles - only font metrics (fontSize, fontWeight, lineHeight, letterSpacing).
 */
data class Auth0Typography(
    // Display styles
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val display: TextStyle,

    // Title styles
    val titleLarge: TextStyle,
//    val titleMedium: TextStyle,
//    val titleSmall: TextStyle,
    val title: TextStyle,

    // Body styles
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val body: TextStyle,

    // Label styles
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val label: TextStyle,

    // Other styles
    val caption: TextStyle,
    val overline: TextStyle,
) {
    companion object {
        /**
         * Creates a default Auth0Typography instance with the specified font family.
         *
         * @param fontFamily The FontFamily to use for all typography styles. Defaults to interFamily.
         */
        fun default(fontFamily: FontFamily = interFamily): Auth0Typography = Auth0Typography(
            displayLarge = TextStyle(
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 41.sp,
                letterSpacing = (-0.2).sp,
                fontFamily = fontFamily
            ),
            displayMedium = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 34.sp,
                letterSpacing = (-0.1).sp,
                fontFamily = fontFamily
            ),
            display = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 28.sp,
                letterSpacing = (-0.05).sp,
                fontFamily = fontFamily
            ),
            titleLarge = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 25.sp,
                letterSpacing = 0.sp,
                fontFamily = fontFamily
            ),
            title = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                fontFamily = fontFamily
            ),
            bodyLarge = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                fontFamily = fontFamily
            ),
            // Body Medium: 14sp, Medium, 17.5sp line, 0.084sp letter
            bodyMedium = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.5.sp,
                letterSpacing = 0.084.sp,
                fontFamily = fontFamily
            ),
            bodySmall = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                letterSpacing = (0.1).sp,
                fontFamily = fontFamily
            ),
            body = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                fontFamily = fontFamily
            ),
            // Label Large: 14sp, SemiBold, 20sp line, 0.084sp letter
            labelLarge = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                letterSpacing = 0.084.sp,
                fontFamily = fontFamily
            ),
            // Label Medium: 12sp, Medium, 16sp line, 0.5sp letter
            labelMedium = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                fontFamily = fontFamily
            ),
            // Label Small: 11sp, Medium, 16sp line, 0.5sp letter
            labelSmall = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                fontFamily = fontFamily
            ),
            // Label: 12sp, Medium, 16sp line, 0.5sp letter
            label = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                letterSpacing = 0.1.sp,
                fontFamily = fontFamily
            ),
            // Caption: 12sp, Normal, 16sp line, 0sp letter
            caption = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
                fontFamily = fontFamily
            ),
            overline = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                letterSpacing = 0.77.sp,
                fontFamily = fontFamily
            )
        )
    }
}
