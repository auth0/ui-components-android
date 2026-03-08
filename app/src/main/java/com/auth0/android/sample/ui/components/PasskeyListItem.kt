package com.auth0.android.sample.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * A list item displaying a passkey entry with device icon, creation date, and delete action.
 *
 * Used in the Profile screen's passkeys section.
 *
 * @param deviceName Name or identifier for the passkey device
 * @param createdDate Date the passkey was created
 * @param icon Device icon (defaults to phone icon)
 * @param showDivider Whether to show a divider below this item
 * @param onDelete Callback when the delete button is tapped
 */
@Composable
fun PasskeyListItem(
    deviceName: String,
    createdDate: String,
    icon: ImageVector = Icons.Default.Info,
    showDivider: Boolean = true,
    onDelete: () -> Unit = {}
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.spacingLg,
                    vertical = dimensions.spacingSm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colors.textDefault
            )

            Spacer(modifier = Modifier.width(dimensions.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = createdDate,
                    style = typography.body,
                    color = colors.textBold
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete passkey",
                    modifier = Modifier.size(20.dp),
                    tint = colors.textDefault
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = dimensions.spacingLg),
                thickness = 0.5.dp,
                color = colors.borderSubtle
            )
        }
    }
}
