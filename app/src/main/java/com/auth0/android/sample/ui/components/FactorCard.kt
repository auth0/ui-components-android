package com.auth0.android.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.auth0.android.sample.ui.theme.isAuth0DarkTheme
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * A radio-selectable card representing an authentication factor.
 *
 * Used in Choose Sign In, Explore Login, and Login & Security screens.
 *
 * @param title Factor name (e.g. "Hosted Login")
 * @param description Short description of the factor
 * @param icon Leading icon for the factor
 * @param isSelected Whether this card is currently selected; shows bold border and filled radio indicator
 * @param onClick Callback when the card is tapped
 */
@Composable
fun FactorCard(
    title: String,
    description: String,
    icon: Painter,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    val isDark = isAuth0DarkTheme()

    // Light: selected=white (layerTop), unselected=near-white (layerMedium)
    // Dark:  both cards use layerMedium (#27272A); selection is shown via border only
    val cardBackground = when {
        isSelected && !isDark -> colors.backgroundLayerTop
        else -> colors.backgroundLayerMedium
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f),
        shape = shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected && isDark -> colors.backgroundAccent  // green #A7F3D0 in dark
                isSelected -> colors.borderBold                  // grey #A1A1AA in light
                else -> colors.borderDefault
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.height(dimensions.spacingXxs))
                Text(
                    text = description,
                    style = typography.body,
                    color = colors.textDefault,
                    minLines = 2
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacingMd))

            // Radio selection indicator:
            //   Light selected: dark filled circle (#09090B) + white inner dot
            //   Dark selected:  green filled circle (#A7F3D0 accent) + dark inner dot
            //   Unselected:     transparent circle with subtle border
            val radioFill = when {
                isSelected && isDark -> colors.backgroundAccent
                isSelected -> colors.backgroundPrimary
                else -> colors.backgroundLayerBase
            }
            val radioBorder = if (isSelected) radioFill else colors.borderDefault
            val radioInnerDot = if (isDark) colors.backgroundLayerMedium else colors.backgroundLayerTop

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = radioBorder,
                        shape = shapes.full
                    )
                    .background(color = radioFill, shape = shapes.full),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = radioInnerDot, shape = shapes.full)
                    )
                }
            }
        }
    }
}
