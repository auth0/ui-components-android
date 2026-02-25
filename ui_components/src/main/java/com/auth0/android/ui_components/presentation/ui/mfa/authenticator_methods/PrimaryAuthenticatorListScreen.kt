package com.auth0.android.ui_components.presentation.ui.mfa.authenticator_methods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.viewmodel.PrimaryAuthenticatorUiData
import com.auth0.android.ui_components.theme.Auth0TokenDefaults

/**
 * Primary Authenticator List Screen
 *
 * Displays the passkey onboarding card and sign-in methods available to the user.
 */
@Composable
fun PrimaryAuthenticatorListScreen(
    primaryAuthenticatorUiData: List<PrimaryAuthenticatorUiData>,
    modifier: Modifier = Modifier,
    onAddPasskeyClick: () -> Unit = {},
    onPasskeysClick: () -> Unit = {}
) {
    val colors = Auth0TokenDefaults.color()

    var isCardDismissed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        if (primaryAuthenticatorUiData.isEmpty() && !isCardDismissed) {
            PasskeyInfoCard(
                onAddPasskeyClick = onAddPasskeyClick,
                onDismissClick = {
                    isCardDismissed = true
                }
            )
        }

        SignInMethodsSection(
            isPasskeyEnrolled = primaryAuthenticatorUiData.isNotEmpty(),
            onPasskeysClick = onPasskeysClick
        )
    }
}

/**
 * Passkey Information Card Component
 *
 * Displays information about passkeys with action buttons
 */
@Composable
private fun PasskeyInfoCard(
    onAddPasskeyClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val shapes = Auth0TokenDefaults.shapes()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 14.dp),
        shape = shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(id = R.string.passkey_info_card_title),
                style = typography.bodyMedium,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(24.dp))


            Text(
                text = stringResource(R.string.what_are_passkeys),
                style = typography.bodyMedium,
                color = colors.textPrimary,
            )

            Text(
                text = stringResource(R.string.passkey_info_card_text_1),
                style = typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.where_are_passkeys_saved),
                style = typography.bodyMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.passkey_info_card_text_2),
                style = typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onAddPasskeyClick,
                shape = shapes.large,
                borderStroke = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                buttonDefaultColor = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.surface, contentColor = colors.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp, pressedElevation = 2.dp
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_passkey),
                    contentDescription = "Passkey icon",
                    modifier = Modifier.size(16.dp),
                    tint = colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.add_passkey),
                    style = typography.bodyLarge,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismissClick()
                    },
                text = stringResource(R.string.dismiss),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                style = typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sign-in Methods Section Component
 *
 * Displays available sign-in methods with navigation
 */
@Composable
private fun SignInMethodsSection(
    isPasskeyEnrolled: Boolean,
    onPasskeysClick: () -> Unit,
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()

    Spacer(modifier = Modifier.height(18.dp))
    Text(
        text = stringResource(R.string.sign_in_methods),
        style = typography.display,
        color = colors.textPrimary,
    )
    Spacer(modifier = Modifier.height(18.dp))

    AuthenticatorItem(
        title = "Passkeys",
        leadingIcon = painterResource(id = R.drawable.ic_passkey),
        showActiveTag = isPasskeyEnrolled,
        onClick = onPasskeysClick
    )
    Spacer(modifier = Modifier.height(24.dp))
}
