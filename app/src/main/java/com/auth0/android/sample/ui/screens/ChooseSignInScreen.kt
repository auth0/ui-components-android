package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.auth0.android.sample.ui.components.Auth0LogoHeader
import com.auth0.android.sample.ui.components.FactorCard
import com.auth0.android.sample.ui.theme.BackGroundColor
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Pre-login screen for choosing the sign-in method.
 *
 * @param onEmbeddedLogin Navigate to embedded login
 * @param onHostedLogin Navigate to hosted (redirect) Auth0 login
 * @param onSettings Navigate to settings/appearance
 */
@Composable
fun ChooseSignInScreen(
    onEmbeddedLogin: () -> Unit, onHostedLogin: () -> Unit, onSettings: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackGroundColor)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Auth0LogoHeader()

        Spacer(modifier = Modifier.height(dimensions.spacingXxl * 4))

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
            onClick = onEmbeddedLogin,
        )

        Spacer(modifier = Modifier.height(dimensions.spacingSm))

        FactorCard(
            title = "Hosted Login",
            description = "Easy to setup, works instantly",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
            onClick = onHostedLogin,
        )

        Spacer(modifier = Modifier.height(dimensions.spacingXxl))

        Row(
            modifier = Modifier
                .padding(dimensions.spacingXs)
                .clickable(onClick = onSettings),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(com.auth0.android.sample.R.drawable.ic_appearance_prefix),
                contentDescription = "Appearance",
                tint = Auth0Theme.colors.backgroundPrimary,
                modifier = Modifier.padding(end = dimensions.spacingXs)
            )
            Text(
                text = "Appearance", style = Auth0Theme.typography.title, color = Auth0Theme.colors.textBold
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingXl))
    }
}
