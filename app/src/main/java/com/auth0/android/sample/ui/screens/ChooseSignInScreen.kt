package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.auth0.android.sample.ui.components.Auth0LogoHeader
import com.auth0.android.sample.ui.components.FactorCard
import com.auth0.android.sample.ui.theme.auth0ScreenBackground
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.theme.Auth0Theme.colors

private enum class LoginOption { Embedded, Hosted }

/**
 * Pre-login screen for choosing the sign-in method.
 *
 * Cards are selectable — tapping a card sets the selection state. The Continue button
 * navigates to the chosen flow and is disabled until a card is selected.
 *
 * @param onEmbeddedLogin Navigate to embedded login
 * @param onHostedLogin Navigate to hosted (redirect) Auth0 login
 * @param onSettings Navigate to settings/appearance
 */
@Composable
fun ChooseSignInScreen(
    onEmbeddedLogin: () -> Unit,
    onHostedLogin: () -> Unit,
    onSettings: () -> Unit = {}
) {
    var selectedOption by remember { mutableStateOf<LoginOption?>(null) }
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val shapes = Auth0Theme.shapes
    val sizes = Auth0Theme.sizes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .auth0ScreenBackground()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensions.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(dimensions.spacingSm))

            Auth0LogoHeader()

            Spacer(modifier = Modifier.height(dimensions.spacingXxl * 2))

            Text(
                text = "Choose how to sign in",
                style = typography.display,
                color = colors.textBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            FactorCard(
                title = "Embedded Login",
                description = "Total brand control and low user frictions",
                icon = painterResource(com.auth0.android.sample.R.drawable.ic_embedded_login),
                isSelected = selectedOption == LoginOption.Embedded,
                onClick = { selectedOption = LoginOption.Embedded }
            )

            Spacer(modifier = Modifier.height(dimensions.spacingMd))

            FactorCard(
                title = "Hosted Login",
                description = "Easy to setup, works instantly",
                icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
                isSelected = selectedOption == LoginOption.Hosted,
                onClick = { selectedOption = LoginOption.Hosted }
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            Button(
                onClick = {
                    when (selectedOption) {
                        LoginOption.Embedded -> onEmbeddedLogin()
                        LoginOption.Hosted -> onHostedLogin()
                        null -> Unit
                    }
                },
                enabled = selectedOption != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight),
                shape = shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.backgroundPrimary,
                    contentColor = colors.textOnPrimary
                )
            ) {
                Text(
                    text = "Continue",
                    style = typography.label
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacingLg))
        }

        // Appearance — pinned at screen bottom, outside the scroll area
        TextButton(
            onClick = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLg, vertical = dimensions.spacingMd)
        ) {
            Icon(
                painter = painterResource(com.auth0.android.sample.R.drawable.ic_appearance_prefix),
                contentDescription = null,
                tint = colors.textBold,
                modifier = Modifier.padding(end = dimensions.spacingXs)
            )
            Text(
                text = "Appearance",
                style = typography.label,
                color = colors.textBold
            )
        }
    }
}
