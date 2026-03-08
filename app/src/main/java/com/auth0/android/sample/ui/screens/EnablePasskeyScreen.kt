package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Passkey information and enrollment screen matching the Figma design.
 *
 * Shows concentric circles graphic, heading, FAQ sections, and CTA buttons.
 *
 * @param onBack Navigate back
 * @param onEnable Callback when user taps "Continue" to enable passkey
 * @param onSkip Callback when user taps "Not now"
 */
@Composable
fun EnablePasskeyScreen(
    onBack: () -> Unit,
    onEnable: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes

    Scaffold(
        topBar = {
            TopBar(title = "", showBackNavigation = true, onBackClick = onBack)
        },
        containerColor = colors.backgroundLayerBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensions.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(dimensions.spacingXl))

            // Concentric circles graphic
            ConcentricCircles(
                modifier = Modifier.size(165.dp),
                color = colors.backgroundPrimary
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXxl))

            Text(
                text = "Enable Passkey",
                style = typography.displayLarge,
                color = colors.textBold
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXl))

            // FAQ: What are passkeys?
            Text(
                text = "What are passkeys?",
                style = typography.titleLarge,
                color = colors.textBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
            Text(
                text = "Passkeys are encrypted digital keys you create using your fingerprint, face, or screen lock.",
                style = typography.body,
                color = colors.textDefault,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            // FAQ: Where are passkeys saved?
            Text(
                text = "Where are passkeys saved?",
                style = typography.titleLarge,
                color = colors.textBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
            Text(
                text = "Passkeys are saved in your credential manager, so you can sign in on other devices.",
                style = typography.body,
                color = colors.textDefault,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight),
                onClick = onEnable
            ) {
                Text(
                    text = "Continue",
                    style = typography.label,
                    color = colors.textOnPrimary
                )
            }

            TextButton(
                onClick = {
                    onSkip()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight)
            ) {
                Text(
                    text = "Not now",
                    style = typography.label,
                    color = colors.textBold
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacingMd))
        }
    }
}

/**
 * Draws concentric circles matching the Figma passkey graphic.
 */
@Composable
private fun ConcentricCircles(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Canvas(modifier = modifier) {
        val center = this.center
        val maxRadius = size.minDimension / 2f
        val rings = 5
        for (i in 0 until rings) {
            val fraction = (i + 1).toFloat() / rings
            val alpha = 0.15f + (0.15f * (rings - i).toFloat() / rings)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = maxRadius * fraction,
                center = center,
                style = Stroke(width = 2f)
            )
        }
        // Center filled circle
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = maxRadius * 0.2f,
            center = center
        )
    }
}
