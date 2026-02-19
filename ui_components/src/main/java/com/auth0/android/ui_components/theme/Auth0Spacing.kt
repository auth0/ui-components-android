package com.auth0.android.ui_components.theme

import androidx.compose.ui.unit.Dp


/**
 * Data class representing the spacing tokens for the Auth0 design system.
 * Defines the space between elements used throughout the design system.
 *
 * @property doubleExtraSmall Default spacing (4dp)
 * @property extraSmall Extra small spacing radius (8dp)
 * @property small Small spacing  (12dp)
 * @property medium Medium spacing  (16dp)
 * @property large Large spacing (24dp )
 * @property extraLarge Extra large spacing (32dp )
 * @property doubleExtraLarge Double Extra Large spacing (48dp)
 */
data class Auth0Spacing(
    val doubleExtraSmall: Dp,
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
    val doubleExtraLarge: Dp
) {
    companion object {
        /**
         * Factory function to create Auth0Spacing with default values.
         *
         * @return Auth0Spacing instance
         */
        fun default(): Auth0Spacing {
            return Auth0Spacing(
                doubleExtraSmall = BASE_DIMEN_VALUE,
                extraSmall = BASE_DIMEN_VALUE * 2,
                small = BASE_DIMEN_VALUE * 3,
                medium = BASE_DIMEN_VALUE * 4,
                large = BASE_DIMEN_VALUE * 6,
                extraLarge = BASE_DIMEN_VALUE * 8,
                doubleExtraLarge = BASE_DIMEN_VALUE * 12
            )
        }
    }
}