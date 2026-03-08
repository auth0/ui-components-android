package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auth0.android.sample.ui.components.Auth0LogoHeader
import com.auth0.android.sample.ui.components.OrDivider
import com.auth0.android.sample.ui.components.SectionHeader
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Embedded login screen matching the Figma design.
 *
 * Shows social login buttons (Google), OR divider, email field,
 * Continue button, and "Sign in with other methods" link.
 *
 * @param onGoogleLogin Callback for Google login
 * @param onContinueWithEmail Callback with email when Continue is tapped
 * @param onOtherMethods Navigate to other sign-in methods
 * @param onBack Navigate back
 */
@Composable
fun EmbeddedLoginScreen(
    onGoogleLogin: () -> Unit = {},
    onContinueWithEmail: (String) -> Unit = {},
    onOtherMethods: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    val shapes = Auth0Theme.shapes

    var email by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Auth0LogoHeader()

        Spacer(modifier = Modifier.height(dimensions.spacingXxl))

        SectionHeader(title = "Login or Signup to continue")

        Spacer(modifier = Modifier.height(dimensions.spacingLg))

        // Social login buttons
        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.buttonHeight),
            shape = shapes.large,
            border = BorderStroke(1.dp, colors.borderDefault)
        ) {
            Text(
                text = "Continue with Google",
                style = typography.label,
                color = colors.textBold
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingMd))

        // OR divider
        OrDivider()

        Spacer(modifier = Modifier.height(dimensions.spacingLg))

        // Email field
        Text(
            text = "Email",
            style = typography.label,
            color = colors.textBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(dimensions.spacingXs))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Your email",
                    style = typography.body,
                    color = colors.textDisabled
                )
            },
            textStyle = typography.body,
            shape = shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.backgroundPrimary,
                unfocusedBorderColor = colors.borderDefault,
                focusedContainerColor = colors.backgroundLayerTop,
                unfocusedContainerColor = colors.backgroundLayerTop
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(dimensions.spacingLg))

        // Continue button
        GradientButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.buttonHeight),
            onClick = { onContinueWithEmail(email) }
        ) {
            Text(
                text = "Continue",
                style = typography.label,
                color = colors.textOnPrimary
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingSm))

        // Other methods link
        TextButton(onClick = onOtherMethods) {
            Text(
                text = "Sign in with other methods",
                style = typography.label,
                color = colors.textBold
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
