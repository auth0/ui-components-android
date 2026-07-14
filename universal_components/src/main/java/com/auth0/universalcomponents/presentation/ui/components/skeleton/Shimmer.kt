package com.auth0.universalcomponents.presentation.ui.components.skeleton

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.auth0.universalcomponents.theme.Auth0Theme

private const val SHIMMER_DURATION_MS = 1300
private const val BAND_WIDTH_MULTIPLIER = 2f
private const val BAND_TRAVEL_MULTIPLIER = 3f
private const val LUMINANCE_DARK_THRESHOLD = 0.5f

// Purpose-built skeleton greys. Declared as properties (not inline literals) so the colour
// hex values are exempt from detekt's MagicNumber rule, mirroring how the theme tokens are
// handled.
private val DarkSkeletonBase = Color(0xFF383838)
private val DarkSkeletonHighlight = Color(0xFF575757)
private val LightSkeletonBase = Color(0xFFE0E0E0)
private val LightSkeletonHighlight = Color(0xFFF7F7F7)

private val DarkSkeletonPalette = SkeletonPalette(base = DarkSkeletonBase, highlight = DarkSkeletonHighlight)
private val LightSkeletonPalette = SkeletonPalette(base = LightSkeletonBase, highlight = LightSkeletonHighlight)

/**
 * Light/dark greys for skeletons. The SDK's layer tokens are near-white-on-white in
 * light mode (an invisible sweep), so we use purpose-built greys that adapt to the
 * colour scheme.
 */
internal data class SkeletonPalette(val base: Color, val highlight: Color) {
    companion object {
        fun forScheme(dark: Boolean): SkeletonPalette =
            if (dark) DarkSkeletonPalette else LightSkeletonPalette
    }
}

/**
 * Resolves the active skeleton palette from the current [Auth0Theme].
 *
 * Uses the resolved theme background's luminance rather than [androidx.compose.foundation.isSystemInDarkTheme]
 * so that a consumer who forces `Auth0Theme(darkTheme = …)` (independent of the system
 * setting) still gets greys that match the rendered surface.
 */
@Composable
internal fun rememberSkeletonPalette(): SkeletonPalette {
    val dark = Auth0Theme.colors.backgroundLayerBase.luminance() < LUMINANCE_DARK_THRESHOLD
    return remember(dark) { SkeletonPalette.forScheme(dark) }
}

/**
 * Sweeps an animated shimmer highlight across this composable to indicate loading.
 * Apply to ONE container (a list/Column) so every child animates in a single
 * synchronized sweep — the highlight is clipped to the content that is actually drawn.
 *
 * When [active] is false, or the system "remove animations" setting is on, this is a
 * no-op and the placeholders render as static base-grey blocks (the reduce-motion
 * fallback).
 */
@Composable
internal fun Modifier.shimmer(active: Boolean = true): Modifier {
    if (!active || !animationsEnabled()) return this

    val palette = rememberSkeletonPalette()
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart, // sweep travels in one direction only
        ),
        label = "shimmer-phase",
    )

    // SrcAtop masks to the content only when that content is drawn into an isolated
    // buffer; without an offscreen layer the band would paint across the whole bounding
    // rect (including the gaps between cards), instead of just the drawn placeholders.
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        // `phase` is read inside the draw lambda → only the draw phase re-runs per frame,
        // not recomposition.
        .drawWithContent {
            drawContent() // 1) draw the real placeholders first
            val w = size.width
            // base → highlight → base band, 2× width, translated by (phase*3 - 2)*w.
            val startX = (phase * BAND_TRAVEL_MULTIPLIER - BAND_WIDTH_MULTIPLIER) * w
            val brush = Brush.linearGradient(
                colors = listOf(palette.base, palette.highlight, palette.base),
                start = Offset(startX, 0f),
                end = Offset(startX + w * BAND_WIDTH_MULTIPLIER, 0f),
            )
            // 2) paint the band ONLY over already-drawn pixels → clips the sweep to the content.
            drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
        }
}

/**
 * Compose has no direct `accessibilityReduceMotion`. Honour the system "remove animations"
 * setting instead: when animations are scaled to 0, skip the sweep (static placeholders).
 */
@Composable
internal fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale != 0f
}
