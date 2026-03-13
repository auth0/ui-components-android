package com.auth0.android.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * A radio-selectable card representing an authentication factor.
 *
 * Used in Choose Sign In, Explore Login, and Login & Security screens.
 *
 * @param title Factor name (e.g. "Hosted Login")
 * @param description Short description of the factor
 * @param icon Leading icon for the factor
 * @param onClick Callback when the card is tapped
 */
@Composable
fun FactorCard(
    title: String,
    description: String,
    icon: Painter,
    onClick: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable { onClick() },
        shape = shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = Auth0Theme.colors.backgroundLayerBase
        ),
        border = BorderStroke(1.dp, colors.borderBold)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMd),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(sizes.iconMedium),
                tint = colors.textBold
            )

            Spacer(modifier = Modifier.width(dimensions.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.title,
                    color = colors.textBold
                )
                Text(
                    text = description,
                    style = typography.body,
                    color = colors.textDefault
                )
            }
        }
    }
}
