package com.auth0.android.ui_components.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Data class representing the dimension/spacing tokens for the Auth0 design system.
 * Defines all spacing values used throughout the design system for consistent layout and spacing.
 *
 * @property spacingXxs Extra extra small spacing (4dp)
 * @property spacingXs Extra small spacing (8dp)
 * @property spacingSm Small spacing (12dp)
 * @property spacingMd Medium spacing (16dp)
 * @property spacingLg Large spacing (24dp)
 * @property spacingXl Extra large spacing (32dp)
 * @property spacingXxl Extra extra large spacing (48dp)
 */
data class Auth0Dimensions(
    val spacingXxs: Dp,
    val spacingXs: Dp,
    val spacingSm: Dp,
    val spacingMd: Dp,
    val spacingLg: Dp,
    val spacingXl: Dp,
    val spacingXxl: Dp
) {
    companion object {
        /**
         * Factory function to create Auth0Dimensions with default values.
         *
         * @return Auth0Dimensions instance with standard spacing values
         */
        fun default(): Auth0Dimensions {
            return Auth0Dimensions(
                spacingXxs = BASE_DIMEN_VALUE,
                spacingXs = BASE_DIMEN_VALUE * 2,
                spacingSm = BASE_DIMEN_VALUE * 3,
                spacingMd = BASE_DIMEN_VALUE * 4,
                spacingLg = BASE_DIMEN_VALUE * 6,
                spacingXl = BASE_DIMEN_VALUE * 8,
                spacingXxl = BASE_DIMEN_VALUE * 12
            )
        }
    }
}


internal val BASE_DIMEN_VALUE = 4.dp
