package com.auth0.universalcomponents.feature.mfachallenge.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.presentation.components.HAIRLINE
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.theme.Auth0Theme

private const val LABEL_MAX_LINES = 2

/**
 * One second factor in the list: its icon, its name and a chevron, on a bordered card; the whole card is the tap
 * target. It grows with large text instead of cutting the name off. While [isBusy] its challenge is going out, so a
 * spinner takes the chevron's place; [enabled] is false while any row is busy.
 */
@Composable
internal fun MfaFactorRow(
    icon: Painter?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isBusy: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = Auth0Theme.colors
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    val shape = Auth0Theme.shapes.large
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sizes.inputHeight)
            .clip(shape)
            .background(colors.backgroundLayerMedium)
            .border(HAIRLINE, colors.borderDefault, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = dimensions.spacingMd + dimensions.spacingXxs, vertical = dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMd),
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = colors.textBold,
                modifier = Modifier.size(sizes.iconMedium),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = Auth0Theme.typography.title,
                color = colors.textBold,
                maxLines = LABEL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = Auth0Theme.typography.bodySmall,
                    color = colors.textDefault,
                )
            }
        }
        if (isBusy) {
            CircularLoader(modifier = Modifier.size(sizes.iconSmall))
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.textBold,
                modifier = Modifier.size(sizes.iconSmall),
            )
        }
    }
}
