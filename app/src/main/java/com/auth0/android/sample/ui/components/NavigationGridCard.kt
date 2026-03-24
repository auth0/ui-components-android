package com.auth0.android.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * A card used in the dashboard 2x3 grid for navigation.
 *
 * Displays an icon and label in a rounded bordered rectangle.
 * Matches the Figma dashboard card design with icon at top-left
 * and label at bottom-left.
 *
 * @param label Card title (e.g. "Profile", "Login & Security")
 * @param icon Icon displayed at the top of the card
 * @param selected Whether this card is in a selected/highlighted state
 * @param onClick Callback when card is tapped
 */
@Composable
fun NavigationGridCard(
    label: String,
    icon: Int,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes

    Card(
        modifier = Modifier
            .height(132.dp)
            .clickable { onClick() },
        shape = shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.backgroundLayerTop),
        border = BorderStroke(1.dp, colors.borderDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensions.spacingLg),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = label,
                modifier = Modifier.size(sizes.iconMedium),
                tint = colors.textBold
            )
            Text(
                text = label,
                style = typography.title,
                color = colors.textBold
            )
        }
    }
}
