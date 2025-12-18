package com.auth0.android.ui_components.styles.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Style configuration for buttons in Auth0 UI components.
 *
 * All properties are optional. When null, the component will use values from the theme.
 *
 * Example:
 * ```
 * Auth0ButtonStyle(
 *     containerColor = Color(0xFF6200EE),
 *     contentColor = Color.White,
 *     shape = RoundedCornerShape(12.dp)
 * )
 * ```
 */
@Immutable
public data class Auth0ButtonStyle(
    /** Background color of the button */
    val containerColor: Color? = null,
    /** Color for text and icons inside the button */
    val contentColor: Color? = null,
    /** Background color when button is disabled */
    val disabledContainerColor: Color? = null,
    /** Content color when button is disabled */
    val disabledContentColor: Color? = null,
    /** Shape/corner radius of the button */
    val shape: Shape? = null,
    /** Text style for button label */
    val textStyle: TextStyle? = null,
    /** Elevation/shadow of the button */
    val elevation: Dp? = null,
    /** Minimum height of the button */
    val minHeight: Dp? = null
) {
    public companion object {
        /**
         * Default button style that uses theme values.
         */
        public val Default: Auth0ButtonStyle = Auth0ButtonStyle()
    }
}
