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

    /** Large display text for hero sections and prominent headings. Inter SemiBold 34sp / 41sp line height / -0.2sp tracking. */
    val displayLarge: TextStyle,
    /** Medium display text for page titles and featured content. Inter SemiBold 28sp / 34sp line height / -0.1sp tracking. */
    val displayMedium: TextStyle,
    /** Standard display text for dialog titles and smaller display contexts. Inter SemiBold 22sp / 28sp line height / -0.05sp tracking. */
    val display: TextStyle,

    // Title styles

    /** Large title text for section headers. Inter SemiBold 20sp / 25sp line height / 0sp tracking. */
    val titleLarge: TextStyle,
    /** Standard title text for card titles and navigation bars. Inter SemiBold 17sp / 24sp line height / 0sp tracking. */
    val title: TextStyle,

    // Body styles

    /** Small body text for secondary content and supporting descriptions. Inter Regular 15sp / 20sp line height / 0.1sp tracking. */
    val bodySmall: TextStyle,
    /** Standard body text for primary content and paragraphs. Inter Regular 17sp / 24sp line height / 0sp tracking. */
    val body: TextStyle,

    // Label styles

    /** Label text for button labels, form fields, and interactive elements. Inter Medium 16sp / 21sp line height / 0.1sp tracking. */
    val label: TextStyle,

    // Other styles

    /** Helper text for hints, validation messages, and supplementary information. Inter Regular 13sp / 18sp line height / 0.2sp tracking. */
    val helper: TextStyle,
    /** Overline text for tags, categories, and small annotations. Inter Regular 11sp / 16sp line height / 0.77sp tracking. */
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
            label = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                letterSpacing = 0.1.sp,
                fontFamily = fontFamily
            ),
            helper = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp,
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
