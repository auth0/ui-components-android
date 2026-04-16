package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auth0.universalcomponents.presentation.ui.components.TopBar
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * User profile screen matching the Figma design.
 *
 * Shows user info header, general personal info section, and app settings section.
 *
 * @param userName Display name of the user
 * @param userEmail Email address of the user
 * @param joinedDate Formatted date string for when the user joined
 * @param currentTheme Current theme name to display
 * @param onBack Navigate back
 * @param onEditName Navigate to Update Full Name screen
 * @param onTheme Navigate to Theme/Appearance screen
 */
@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    joinedDate: String? = null,
    currentTheme: String = "Automatic",
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onTheme: () -> Unit
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val shapes = Auth0Theme.shapes

    Scaffold(
        topBar = {
            TopBar(title = "", showBackNavigation = true, onBackClick = onBack)
        },
        containerColor = colors.backgroundLayerBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensions.spacingLg)
        ) {
            // User info header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensions.spacingXxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = userName,
                    style = typography.displayLarge,
                    color = colors.textBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(dimensions.spacingXs))
                Text(
                    text = "Joined 1 year ago",
                    style = typography.body,
                    color = colors.textDefault,
                    textAlign = TextAlign.Center
                )
            }

            // General section
            Text(
                text = "General",
                style = typography.titleLarge,
                color = colors.textBold,
            )

            Spacer(modifier = Modifier.height(dimensions.spacingMd))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colors.backgroundLayerTop,
                        shape = shapes.large
                    )
            ) {
                ProfileInfoRow(
                    icon = Icons.Outlined.Person,
                    text = userName,
                    onClick = null
                )
                if (userEmail.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = dimensions.spacingLg),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.Email,
                        text = userEmail
                    )
                }
                if (joinedDate != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = dimensions.spacingLg),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.DateRange,
                        text = joinedDate
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacingXl))

            // App Setting section
            Text(
                text = "App Setting",
                style = typography.titleLarge,
                color = colors.textBold
            )
            Spacer(modifier = Modifier.height(dimensions.spacingMd))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colors.backgroundLayerTop,
                        shape = shapes.large
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTheme() }
                        .padding(
                            horizontal = dimensions.spacingLg,
                            vertical = dimensions.spacingMd
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Theme",
                        style = typography.body,
                        color = colors.textBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = currentTheme,
                        style = typography.body,
                        color = colors.textDefault
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textDefault
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacingLg))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(
                horizontal = dimensions.spacingLg,
                vertical = dimensions.spacingMd
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textDefault,
            modifier = Modifier.size(sizes.iconMedium)
        )
        Spacer(modifier = Modifier.width(dimensions.spacingMd))
        Text(
            text = text,
            style = typography.body,
            color = colors.textBold,
            modifier = Modifier.weight(1f)
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textDefault
            )
        }
    }
}
