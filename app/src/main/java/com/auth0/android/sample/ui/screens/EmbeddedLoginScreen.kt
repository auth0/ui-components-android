package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.auth0.android.sample.R
import com.auth0.android.sample.ui.components.Auth0LogoHeader
import com.auth0.android.sample.ui.components.OrDivider
import com.auth0.android.sample.ui.theme.auth0ScreenBackground
import com.auth0.android.sample.ui.theme.isAuth0DarkTheme
import com.auth0.android.sample.ui.components.SampleGradientButton
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Embedded login screen matching the Figma design.
 *
 * Shows social login buttons (Google), OR divider, email field,
 * Continue button, and "Sign in with other methods" link.
 *
 * @param onGoogleLogin Callback for Google login
 * @param onContinueWithEmail Callback with email and password when Continue is tapped
 * @param onOtherMethods Navigate to other sign-in methods
 * @param onBack Navigate back
 */
@Composable
fun EmbeddedLoginScreen(
    onGoogleLogin: () -> Unit = {},
    onContinueWithEmail: (String, String) -> Unit = { _, _ -> },
    onOtherMethods: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    val shapes = Auth0Theme.shapes
    val isDark = isAuth0DarkTheme()
    val fieldFocusedBorder = if (isDark) colors.backgroundAccent else colors.backgroundPrimary

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .auth0ScreenBackground()
            .statusBarsPadding()
            .padding(horizontal = dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textBold
                )
            }
        }

        Auth0LogoHeader()

        Spacer(modifier = Modifier.height(dimensions.spacingXxl))

        Text(
            text = "Login or Signup to continue",
            style = typography.displayMedium,
            color = colors.textBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(dimensions.spacingLg))

        // Social login buttons
        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.buttonHeight),
            shape = shapes.large,
            border = BorderStroke(1.dp, colors.borderDefault),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isDark) colors.backgroundLayerMedium else colors.backgroundLayerTop
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(sizes.iconSmall)
            )
            Spacer(modifier = Modifier.width(dimensions.spacingXs))
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
            shape = shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = fieldFocusedBorder,
                unfocusedBorderColor = colors.borderDefault,
                focusedContainerColor = if (isDark) colors.backgroundLayerMedium else colors.backgroundLayerTop,
                unfocusedContainerColor = if (isDark) colors.backgroundLayerMedium else colors.backgroundLayerTop,
                focusedTextColor = colors.textBold,
                unfocusedTextColor = colors.textBold,
                cursorColor = fieldFocusedBorder
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(dimensions.spacingMd))

        // Password field
        Text(
            text = "Password",
            style = typography.label,
            color = colors.textBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(dimensions.spacingXs))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Password",
                    style = typography.body,
                    color = colors.textDisabled
                )
            },
            textStyle = typography.body,
            shape = shapes.large,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = if (passwordVisible) painterResource(R.drawable.ic_password_visible) else painterResource(
                            R.drawable.ic_password_hidden
                        ),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colors.textDefault
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = fieldFocusedBorder,
                unfocusedBorderColor = colors.borderDefault,
                focusedContainerColor = if (isDark) colors.backgroundLayerMedium else colors.backgroundLayerTop,
                unfocusedContainerColor = if (isDark) colors.backgroundLayerMedium else colors.backgroundLayerTop,
                focusedTextColor = colors.textBold,
                unfocusedTextColor = colors.textBold,
                cursorColor = fieldFocusedBorder
            ),
            singleLine = true
        )


        Spacer(modifier = Modifier.height(dimensions.spacingLg))

        // Continue button
        SampleGradientButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.buttonHeight),
            onClick = { onContinueWithEmail(email, password) }
        ) {
            Text(
                text = "Continue",
                style = typography.label,
                color = colors.textOnPrimary
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingSm))
        Spacer(modifier = Modifier.weight(1f))
    }
}
