package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.theme.Auth0Theme

private val HAIRLINE = 1.dp

private const val GLOW_ALPHA = 0.07f
private const val GLOW_CENTER_Y = 0.85f
private const val GLOW_RADIUS = 0.75f
private const val ERROR_BORDER_ALPHA = 0.18f

/** The white, softly lifted surface behind a field; lifts further when focused. */
@Composable
internal fun Modifier.loginSurface(shape: Shape, isFocused: Boolean, isError: Boolean): Modifier {
    val colors = Auth0Theme.colors
    val elevation by animateDpAsState(
        targetValue = if (isFocused) Auth0Theme.dimensions.spacingXxs else HAIRLINE,
        label = "surfaceElevation",
    )
    val border by animateColorAsState(
        targetValue = when {
            isError -> colors.textOnError.copy(alpha = ERROR_BORDER_ALPHA)
            isFocused -> colors.borderDefault
            else -> colors.borderSubtle
        },
        label = "surfaceBorder",
    )
    val fill by animateColorAsState(
        targetValue = if (isError) colors.backgroundErrorSubtle else colors.backgroundLayerTop,
        label = "surfaceFill",
    )
    return this
        .shadow(elevation, shape, ambientColor = colors.borderShadow, spotColor = colors.borderShadow)
        .background(fill, shape)
        .border(HAIRLINE, border, shape)
}

/** The base background with a soft brand glow along the bottom. */
@Composable
internal fun Modifier.loginBackground(): Modifier {
    val base = Auth0Theme.colors.backgroundLayerBase
    val glow = Auth0Theme.colors.backgroundAccent.copy(alpha = GLOW_ALPHA)
    return this
        .background(base)
        .drawBehind {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    center = Offset(size.width / 2, size.height * GLOW_CENTER_Y),
                    radius = size.width * GLOW_RADIUS,
                )
            )
        }
}
