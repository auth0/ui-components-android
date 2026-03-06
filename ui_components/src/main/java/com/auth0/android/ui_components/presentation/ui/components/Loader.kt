package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.Auth0Theme


@Composable
public fun CircularLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 2.dp
) {
    val resolvedColor = if (color == Color.Unspecified) Auth0Theme.colors.backgroundPrimary else color

    CircularProgressIndicator(
        modifier = modifier,
        color = resolvedColor,
        strokeWidth = strokeWidth
    )
}