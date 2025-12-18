package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorSettingsComponent
import com.auth0.android.ui_components.styles.components.Auth0ButtonStyle
import com.auth0.android.ui_components.styles.components.AuthenticatorItemStyle
import com.auth0.android.ui_components.styles.components.OTPInputStyle
import com.auth0.android.ui_components.styles.components.TopBarStyle
import com.auth0.android.ui_components.styles.screens.AuthenticatorMethodsScreenStyle
import com.auth0.android.ui_components.theme.ActiveLabelText
import com.auth0.android.ui_components.theme.Auth0ColorScheme
import com.auth0.android.ui_components.theme.Auth0UiThemeConfig
import com.auth0.android.ui_components.theme.ScreenStyles


private val customAuth0Theme = Auth0UiThemeConfig(


    // Screen-specific styles
    screenStyles = ScreenStyles(
        // Authenticator Methods Screen customization
        authenticatorMethods = AuthenticatorMethodsScreenStyle(
            backgroundColor = Auth0ColorScheme().background,
            listItemStyle = AuthenticatorItemStyle(
                backgroundColor = Color.Red,
                activeTagBackgroundColor = ActiveLabelText,
                activeTagTextColor = Color.White
            ),

//            topBarStyle = TopBarStyle(
//                backgroundColor = Auth0ColorScheme().primary,
//                titleStyle = TextStyle(
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 28.sp,
//                    color = Color(0xFF2D3142)
//                ),
//                showDivider = true,
//            ),
            horizontalPadding = 20.dp
        ),

        // OTP Verification Screen customization
        otpVerification = OTPVerificationScreenStyle(
            backgroundColor = Color(0xFFF8F9FE),
            topBarStyle = TopBarStyle(
                backgroundColor = Color.White,
                titleStyle = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF2D3142)
                ),
                showDivider = false
            ),
            titleStyle = TextStyle(
                fontWeight = FontWeight.Thin,
                fontSize = 22.sp,
                color = Color(0xFF2D3142)
            ),
            descriptionStyle = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            ),
            otpInputStyle = OTPInputStyle(
                cellBackgroundColor = Color.White,
                cellBorderColor = Color(0xFFD1D5DB),
                cellFocusedBorderColor = Color(0xFF6C63FF),
                cellErrorBorderColor = Color(0xFFE53935),
                cellShape = RoundedCornerShape(12.dp),
                spacing = 10.dp,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF2D3142)
                )
            ),
            buttonStyle = Auth0ButtonStyle(
                containerColor = Color(0xFF6C63FF),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp)
            ),
            errorTextColor = Color(0xFFE53935),
            horizontalPadding = 20.dp,
            verticalPadding = 32.dp
        )
    )
)

@Composable
fun Settings() {
    // Pass the custom theme configuration to test theming behavior
    AuthenticatorSettingsComponent(
        themeConfig = customAuth0Theme
    )
}

/**
 * Alternative: Use default theme (uncomment to test default behavior)
 */
//@Composable
//fun Settings() {
//    AuthenticatorSettingsComponent()
//}