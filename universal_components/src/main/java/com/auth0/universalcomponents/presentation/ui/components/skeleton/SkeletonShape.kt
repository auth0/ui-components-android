package com.auth0.universalcomponents.presentation.ui.components.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default height of a single text-line placeholder. */
private val SkeletonLineHeight = 14.dp

/**
 * A single themed placeholder block — the atom of the skeleton system. Compose several
 * inside Rows/Columns to mirror a real layout, then wrap the parent in Modifier.shimmer().
 * Renders the resting base grey; the sweep is layered on by the parent's shimmer().
 */
@Composable
internal fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val base = rememberSkeletonPalette().base
    Box(modifier.background(color = base, shape = shape))
}

/** Convenience: a single text-line placeholder (default 14.dp tall). */
@Composable
internal fun SkeletonLine(width: Dp, modifier: Modifier = Modifier) {
    SkeletonBox(modifier.width(width).height(SkeletonLineHeight))
}
