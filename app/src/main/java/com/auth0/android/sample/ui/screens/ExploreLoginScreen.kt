package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.auth0.android.sample.ui.components.Auth0LogoHeader
import com.auth0.android.sample.ui.components.FactorCard
import com.auth0.android.sample.ui.components.SectionHeader
import com.auth0.android.sample.ui.theme.BackGroundColor
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Explore login experience screen matching the Figma design.
 *
 * Shows available login methods as selectable cards, plus locked
 * methods that require additional tenant configuration.
 *
 * @param onBack Navigate back
 */
@Composable
fun ExploreLoginScreen(
    onBack: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackGroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Auth0LogoHeader()

        Spacer(modifier = Modifier.height(dimensions.spacingXl))

        SectionHeader(
            title = "Explore different login experience",
            subtitle = "Depends on your current tenant setting, these experiences are available to you"
        )

        Spacer(modifier = Modifier.height(dimensions.spacingLg))

        // Available methods
        FactorCard(
            title = "Hosted Login",
            description = "Easy to setup, works instantly",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
            onClick = { })
        Spacer(modifier = Modifier.height(dimensions.spacingMd))
        FactorCard(
            title = "Biometric / Passkey",
            description = "FIDO2 / WebAuthn via biometrics",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
            onClick = { })
        Spacer(modifier = Modifier.height(dimensions.spacingMd))
        FactorCard(
            title = "Embedded Login",
            description = "Social + email/password login",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
            onClick = { })
        Spacer(modifier = Modifier.height(dimensions.spacingMd))
        FactorCard(
            title = "Native Social Login",
            description = "Google Sign-In native integration",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
            onClick = { })

        Spacer(modifier = Modifier.height(dimensions.spacingXl))

        // Locked methods section
        SectionHeader(
            title = "Unlock more experiences",
            subtitle = "These experiences require additional configurations in your tenant."
        )
        Spacer(modifier = Modifier.height(dimensions.spacingMd))

        FactorCard(
            title = "MFA",
            description = "Challenge for sensitive actions.",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSm))
        TextButton(
            onClick = { /* Open MFA setup docs */ }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "How to setup MFA", style = typography.label, color = colors.textBold
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingMd))

        FactorCard(
            title = "SMS OTP",
            description = "Passwordless flow for SMS OTP",
            icon = painterResource(com.auth0.android.sample.R.drawable.ic_hosted_login),
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSm))
        TextButton(
            onClick = { /* Open SMS setup docs */ }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "How to setup Phone for SMS",
                style = typography.label,
                color = colors.textBold
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingLg))
    }
}
