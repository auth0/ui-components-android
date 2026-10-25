package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import com.auth0.universalcomponents.theme.Auth0Theme

/** One second factor in the list: its icon, its name and a chevron; the whole row is the tap target. */
@Composable
internal fun MfaFactorRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Auth0Theme.colors
    val sizes = Auth0Theme.sizes
    val shape = Auth0Theme.shapes.medium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.inputHeight)
            .loginSurface(shape, isFocused = false, isError = false)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = sizes.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.textDefault,
            modifier = Modifier.size(sizes.iconMedium),
        )
        Spacer(Modifier.width(Auth0Theme.dimensions.spacingMd))
        Text(
            text = label,
            style = Auth0Theme.typography.label,
            color = colors.textBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textBold,
            modifier = Modifier.size(sizes.iconSmall),
        )
    }
}
