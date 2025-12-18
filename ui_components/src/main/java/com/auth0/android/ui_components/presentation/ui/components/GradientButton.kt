package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.styles.components.Auth0ButtonStyle
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Button component with gradient overlay and theme support.
 *
 * @param modifier Modifier for the button
 * @param style Optional Auth0ButtonStyle for customization. When provided, overrides buttonDefaultColor and shape.
 * @param gradient Gradient brush overlay
 * @param buttonDefaultColor Button colors (used when style is not provided)
 * @param shape Button shape (used when style is not provided)
 * @param elevation Button elevation
 * @param isLoading Whether to show loading state
 * @param enabled Whether the button is enabled
 * @param borderStroke Optional border stroke
 * @param onClick Callback when button is clicked
 * @param content Button content
 */
@Composable
internal fun GradientButton(
    modifier: Modifier = Modifier,
    style: Auth0ButtonStyle? = null,
    gradient: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.Transparent
        )
    ),
    buttonDefaultColor: ButtonColors? = null,
    shape: Shape? = null,
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 2.dp
    ),
    isLoading: Boolean = false,
    enabled: Boolean = true,
    borderStroke: BorderStroke? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    // Resolve styles with priority: style parameter -> individual parameters -> theme defaults
    val containerColor = style?.containerColor ?: Auth0Theme.colors.primary
    val contentColor = style?.contentColor ?: Auth0Theme.colors.onPrimary
    val disabledContainerColor = style?.disabledContainerColor ?: containerColor.copy(alpha = 0.6f)
    val disabledContentColor = style?.disabledContentColor ?: contentColor.copy(alpha = 0.6f)
    val buttonShape: Shape = style?.shape ?: shape ?: Auth0Theme.shapes.large

    // If buttonDefaultColor is provided, use it directly (backward compatibility)
    val buttonColors = buttonDefaultColor ?: ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
    
    // Resolve content color for loading indicator
    val resolvedContentColor = buttonDefaultColor?.contentColor ?: contentColor

    Button(
        modifier = modifier,
        colors = buttonColors,
        shape = buttonShape,
        contentPadding = PaddingValues(),
        elevation = elevation,
        enabled = enabled && !isLoading,
        border = borderStroke,
        onClick = { onClick() },
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularLoader(
                        modifier = Modifier.size(16.dp),
                        color = resolvedContentColor.copy(alpha = 0.75f),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                } else {
                    content()
                }
            }
        }
    }
}