package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.presentation.ui.flow.HAIRLINE
import com.auth0.universalcomponents.theme.Auth0Theme

private const val DISABLED_ALPHA = 0.4f
private val SELECTED_BORDER = 2.dp

/**
 * One choice of a few, such as "Text message" or "Voice call", as tall as the other buttons on its screen (at least
 * `sizes.buttonHeight`, and taller at a large font). The chosen one has a bold border; a screen reader hears it as
 * selected. Put the choices in a row or column marked with `selectableGroup()`. In a row, give the row
 * `IntrinsicSize.Min` and each button `fillMaxHeight()`, so a name that wraps at a large font keeps them even. It is
 * off, and fades, while [enabled] is false.
 */
@Composable
internal fun LoginChoiceButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Auth0Theme.colors
    val shape = Auth0Theme.shapes.medium
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .heightIn(min = Auth0Theme.sizes.buttonHeight)
            .clip(shape)
            .background(colors.backgroundLayerTop)
            .border(
                width = if (isSelected) SELECTED_BORDER else HAIRLINE,
                color = if (isSelected) colors.textBold else colors.borderDefault,
                shape = shape,
            )
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(Auth0Theme.dimensions.spacingSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = Auth0Theme.typography.label,
            color = colors.textBold,
            textAlign = TextAlign.Center,
        )
    }
}
