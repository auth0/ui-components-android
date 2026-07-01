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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.theme.Auth0Theme

private val AuthMethodCardHeight = 70.dp
private val EnrolledCardHeight = 84.dp
private val EnrolledCardPadding = 20.dp
private val AuthMethodTitleWidth = 140.dp
private val EnrolledTitleWidth = 180.dp
private val EnrolledSubtitleWidth = 120.dp

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

/** Mirrors EnrolledAuthenticatorItem: title + "created on" line + trailing menu glyph. */
@Composable
internal fun EnrolledAuthenticatorCardSkeleton() = SkeletonCard(
    height = EnrolledCardHeight,
    contentPadding = EnrolledCardPadding,
    verticalAlignment = Alignment.Top,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Auth0Theme.dimensions.spacingXxs)) {
        SkeletonLine(width = EnrolledTitleWidth)
        SkeletonLine(width = EnrolledSubtitleWidth)
    }
    Spacer(Modifier.weight(1f))
    SkeletonBox(Modifier.size(Auth0Theme.sizes.iconMedium), shape = Auth0Theme.shapes.small)
}

// region Previews

@Preview(name = "Auth method skeleton · light", showBackground = true)
@Composable
private fun AuthMethodSkeletonLightPreview() {
    Auth0Theme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Auth0Theme.sizes.padding)
                .shimmer(),
        ) {
            SkeletonLine(width = EnrolledTitleWidth, modifier = Modifier.height(22.dp))
            Spacer(Modifier.height(Auth0Theme.dimensions.spacingMd))
            SkeletonList(count = 3) { AuthMethodCardSkeleton() }
        }
    }
}

@Preview(name = "Auth method skeleton · dark", showBackground = true)
@Composable
private fun AuthMethodSkeletonDarkPreview() {
    Auth0Theme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Auth0Theme.sizes.padding)
                .shimmer(),
        ) {
            SkeletonLine(width = EnrolledTitleWidth, modifier = Modifier.height(22.dp))
            Spacer(Modifier.height(Auth0Theme.dimensions.spacingMd))
            SkeletonList(count = 3) { AuthMethodCardSkeleton() }
        }
    }
}

@Preview(name = "Enrolled skeleton · light", showBackground = true)
@Composable
private fun EnrolledSkeletonLightPreview() {
    Auth0Theme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Auth0Theme.sizes.padding)
                .shimmer(),
        ) {
            SkeletonList(count = 3, spacing = Auth0Theme.dimensions.spacingSm) {
                EnrolledAuthenticatorCardSkeleton()
            }
        }
    }
}

@Preview(name = "Enrolled skeleton · dark", showBackground = true)
@Composable
private fun EnrolledSkeletonDarkPreview() {
    Auth0Theme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Auth0Theme.sizes.padding)
                .shimmer(),
        ) {
            SkeletonList(count = 3, spacing = Auth0Theme.dimensions.spacingSm) {
                EnrolledAuthenticatorCardSkeleton()
            }
        }
    }
}

// endregion
