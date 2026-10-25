package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.auth0.universalcomponents.theme.Auth0Theme

/** The page title, with an optional subtitle and "Edit" action. [bottomSpacing] overrides the gap to the content. */
@Composable
internal fun LoginScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    editLabel: String? = null,
    onEdit: (() -> Unit)? = null,
    bottomSpacing: Dp? = null,
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = typography.body, color = colors.textBold)
        if (subtitle != null) {
            Spacer(Modifier.height(dimensions.spacingXs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = typography.body,
                    color = colors.textDefault,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (editLabel != null && onEdit != null) {
                    Spacer(Modifier.width(dimensions.spacingSm))
                    Text(
                        text = editLabel,
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textBold,
                        modifier = Modifier.clickable(onClick = onEdit),
                    )
                }
            }
        }
        val gap = bottomSpacing ?: if (subtitle == null) {
            dimensions.spacingXl + dimensions.spacingXxs
        } else {
            dimensions.spacingXxl + dimensions.spacingXs
        }
        Spacer(Modifier.height(gap))
    }
}
