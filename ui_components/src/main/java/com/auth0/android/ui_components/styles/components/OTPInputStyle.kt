package com.auth0.android.ui_components.styles.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Style configuration for OTP input fields.
 *
 * All properties are optional. When null, the component will use values from the theme.
 *
 * Example:
 * ```
 * OTPInputStyle(
 *     cellBackgroundColor = Color.White,
 *     cellBorderColor = Color.Gray,
 *     cellFocusedBorderColor = Color.Blue,
 *     cellErrorBorderColor = Color.Red,
 *     spacing = 8.dp
 * )
 * ```
 */
@Immutable
public data class OTPInputStyle(
    /** Background color of each OTP cell */
    val cellBackgroundColor: Color? = null,
    /** Border color of each OTP cell */
    val cellBorderColor: Color? = null,
    /** Border color when cell is focused */
    val cellFocusedBorderColor: Color? = null,
    /** Border color when there's an error */
    val cellErrorBorderColor: Color? = null,
    /** Shape of each OTP cell */
    val cellShape: Shape? = null,
    /** Spacing between OTP cells */
    val spacing: Dp? = null,
    /** Border width of each cell */
    val borderWidth: Dp? = null,
    /** Text style for the OTP digits */
    val textStyle: TextStyle? = null
) {
    public companion object {
        /**
         * Default OTP input style that uses theme values.
         */
        public val Default: OTPInputStyle = OTPInputStyle()
    }
}
