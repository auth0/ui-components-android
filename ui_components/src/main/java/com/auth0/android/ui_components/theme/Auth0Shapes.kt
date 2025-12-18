package com.auth0.android.ui_components.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape scheme for Auth0 UI components.
 *
 * Provides consistent corner radius values for UI elements.
 * All parameters have default values, so you only need to specify what you want to customize.
 *
 * Example:
 * ```
 * Auth0Shapes(
 *     medium = RoundedCornerShape(16.dp),
 *     large = RoundedCornerShape(24.dp)
 * )
 * ```
 */
@Immutable
public data class Auth0Shapes(
    /** No rounding, sharp corners */
    val none: Shape = RoundedCornerShape(0.dp),
    /** Extra small rounding (4dp) */
    val extraSmall: Shape = RoundedCornerShape(4.dp),
    /** Small rounding (8dp) */
    val small: Shape = RoundedCornerShape(8.dp),
    /** Medium rounding (12dp) */
    val medium: Shape = RoundedCornerShape(12.dp),
    /** Large rounding (16dp)  */
    val large: Shape = RoundedCornerShape(16.dp),
    /** Extra large rounding (24dp)  */
    val extraLarge: Shape = RoundedCornerShape(24.dp),
    /** Full rounding (50%) */
    val full: Shape = RoundedCornerShape(50)
)
