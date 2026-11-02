package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * A centred text action under the main button, like "Choose another device" or Resend; the full width is the tap
 * target. Its colour fades between enabled and disabled.
 */
@Composable
internal fun LoginTextButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = Auth0Theme.typography.label,
) {
    val colors = Auth0Theme.colors
    val color by animateColorAsState(
        targetValue = if (enabled) colors.textBold else colors.textDisabled,
        label = "textButtonColor",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(Auth0Theme.shapes.large)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, style = style, color = color)
    }
}
