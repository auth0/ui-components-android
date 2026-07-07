package com.auth0.universalcomponents.presentation.ui.components.skeleton

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.theme.Auth0Theme

private val AuthMethodCardHeight = 70.dp
private val AuthMethodTitleWidth = 140.dp

/**
 * Repeats a skeleton [row] [count] times. Intentionally does NOT shimmer itself — apply
 * Modifier.shimmer() ONCE on the screen's loading container (see the screens) so the header
 * line and every card animate in a single synchronized sweep.
 */
@Composable
internal fun SkeletonList(
    count: Int = 5,
    modifier: Modifier = Modifier,
    spacing: Dp = Auth0Theme.dimensions.spacingMd,
    row: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(count) { row() }
    }
}

/**
 * Border-only card shell matching the real cards' size, border, and radius (NO fill —
 * a grey sweep over the near-white `backgroundLayerMedium` fill would read as a dark band
 * crossing a white card). Size, border, and radius still match the real card so there's no
 * layout shift when data arrives. Parametrised because the two real cards differ in
 * height/padding.
 */
@Composable
internal fun SkeletonCard(
    height: Dp,
    contentPadding: Dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Auth0Theme.colors.borderDefault, Auth0Theme.shapes.large)
            .padding(contentPadding),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/** Mirrors AuthenticatorItem: leading icon, title line, trailing chevron. */
@Composable
internal fun AuthMethodCardSkeleton() = SkeletonCard(
    height = AuthMethodCardHeight,
    contentPadding = Auth0Theme.sizes.padding, // 16.dp
) {
    SkeletonBox(Modifier.size(Auth0Theme.sizes.iconMedium), shape = Auth0Theme.shapes.small)
    Spacer(Modifier.width(Auth0Theme.dimensions.spacingMd))
    SkeletonLine(width = AuthMethodTitleWidth)
    Spacer(Modifier.weight(1f))
    SkeletonBox(Modifier.size(Auth0Theme.sizes.iconMedium), shape = Auth0Theme.shapes.small)
}
