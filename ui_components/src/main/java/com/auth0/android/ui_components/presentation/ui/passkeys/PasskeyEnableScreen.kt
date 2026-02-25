package com.auth0.android.ui_components.presentation.ui.passkeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auth0.android.ui_components.R
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.theme.Auth0TokenDefaults

/**
 * Passkey Enable Screen
 *
 * Displays information about passkeys and allows users to enable passkey authentication.
 */
@Composable
fun PasskeyEnableScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEnableClick: () -> Unit = {}
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()
    val shapes = Auth0TokenDefaults.shapes()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.passkeys_title),
                onBackClick = onBackClick
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.Center
        ) {
            PasskeyIcon()

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.enable_passkey),
                style = typography.displayMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "\u2022  ${stringResource(R.string.what_are_passkeys)}",
                style = typography.bodyLarge,
                color = colors.textPrimary
            )
            Text(
                modifier = Modifier.padding(vertical = 6.dp),
                text = stringResource(R.string.passkeys_description),
                style = typography.bodyLarge,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "\u2022  ${stringResource(R.string.where_are_passkeys_saved)}",
                style = typography.bodyLarge,
                color = colors.textPrimary
            )
            Text(
                modifier = Modifier.padding(vertical = 6.dp),
                text = stringResource(R.string.passkeys_saved_description),
                style = typography.bodyLarge,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onEnableClick,
                shape = shapes.large,
                buttonDefaultColor = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                gradient = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 2.dp
                ),
                isLoading = false
            ) {
                Text(
                    text = stringResource(R.string.enable),
                    style = typography.bodyLarge,
                    color = colors.onPrimary
                )
            }
        }
    }
}

/**
 * Passkey Icon Component
 *
 * Displays the concentric circles passkey icon
 */
@Composable
private fun PasskeyIcon() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        repeat(5) { index ->
            val sizes = listOf(165.dp, 139.62.dp, 114.23.dp, 88.85.dp, 63.46.dp)
            Box(
                modifier = Modifier
                    .size(sizes[index])
                    .clip(CircleShape)
                    .background(
                        color = Color(0xFFB1E4DE).copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_passkey),
            contentDescription = stringResource(R.string.passkeys_title),
            modifier = Modifier.size(48.dp),
            tint = Auth0TokenDefaults.color().foreground
        )
    }
}
