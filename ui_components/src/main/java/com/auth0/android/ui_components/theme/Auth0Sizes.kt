package com.auth0.android.ui_components.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Auth0 size tokens defining standard component dimensions.
 *
 * Provides consistent sizing for buttons, inputs, OTP fields, code containers, and icons
 * across the design system.
 */
data class Auth0Sizes(
    /** Standard height for primary and secondary buttons. */
    val buttonHeight: Dp,
    /** Standard height for text input fields. */
    val inputHeight: Dp,
    /** Width of individual OTP digit input fields. */
    val otpFieldWidth: Dp,
    /** Height of individual OTP digit input fields. */
    val otpFieldHeight: Dp,
    /** Height for recovery code display containers. */
    val codeContainerHeight: Dp,
    /** Small icon size for inline or secondary icons. */
    val iconSmall: Dp,
    /** Medium icon size for standard interactive icons. */
    val iconMedium: Dp,
    /** Large icon size for prominent or featured icons. */
    val iconLarge: Dp,
    /** Default padding value used */
    val padding: Dp,

    /**
     *  Larger padding value
     */
    val paddingLarge: Dp
) {
    companion object {
        fun default(): Auth0Sizes = Auth0Sizes(
            buttonHeight = 56.dp,
            inputHeight = 68.dp,
            otpFieldWidth = 52.dp,
            otpFieldHeight = 60.dp,
            codeContainerHeight = 56.dp,
            iconSmall = 16.dp,
            iconMedium = 24.dp,
            iconLarge = 32.dp,
            padding = 16.dp,
            paddingLarge = 24.dp
        )
    }
}
