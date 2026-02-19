package com.auth0.android.ui_components.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Data class representing the shape tokens for the Auth0 design system.
 * Defines all corner radius values used throughout the design system.
 *
 * @property none No rounding (0dp corners)
 * @property extraSmall Extra small corner radius (4dp)
 * @property small Small corner radius (8dp)
 * @property medium Medium corner radius (12dp)
 * @property large Large corner radius (16dp for Default/Minimal, 20dp for Rounded)
 * @property extraLarge Extra large corner radius (24dp for Default/Minimal, 32dp for Rounded)
 * @property full Full corner radius (100dp for pill/circular shapes)
 */
data class Auth0Shapes(
    val none: RoundedCornerShape,
    val extraSmall: RoundedCornerShape,
    val small: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape,
    val extraLarge: RoundedCornerShape,
    val full: RoundedCornerShape
) {
    companion object {
        /**
         * Factory function to create Auth0Shapes with default values.
         *
         * @return Auth0Shapes instance with values
         */
        fun default(): Auth0Shapes {
            return Auth0Shapes(
                none = RoundedCornerShape(0.dp),
                extraSmall = RoundedCornerShape(BASE_DIMEN_VALUE),
                small = RoundedCornerShape(BASE_DIMEN_VALUE * 2),
                medium = RoundedCornerShape(BASE_DIMEN_VALUE * 3),
                large = RoundedCornerShape(BASE_DIMEN_VALUE * 4),
                extraLarge = RoundedCornerShape(BASE_DIMEN_VALUE * 6),
                full = RoundedCornerShape(BASE_DIMEN_VALUE * 25)
            )
        }
    }
}
