package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.auth0.android.sample.ui.theme.ThemePreset
import com.auth0.android.ui_components.theme.Auth0Dimensions
import com.auth0.android.ui_components.theme.Auth0Shapes

/**
 * Theme selection screen displayed before the AuthenticatorSettingsComponent.
 *
 * Shows the 6 pre-built theme presets as selectable cards. When a preset is
 * tapped, the app navigates to Settings with that theme applied.
 *
 * @param onThemeSelected Callback with the index of the selected preset
 */
@Composable
fun ThemeSelectionScreen(onThemeSelected: (Int) -> Unit) {
    val dimensions = Auth0Dimensions.default()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSm)
    ) {
        item {
            Spacer(modifier = Modifier.height(dimensions.spacingLg))
            Text(
                text = "Select a Theme",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXxs))
            Text(
                text = "Choose a theme to apply to the Authenticator Settings component.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
        }

        itemsIndexed(ThemePreset.all()) { index, preset ->
            ThemeSelectionCard(
                preset = preset,
                onClick = { onThemeSelected(index) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(dimensions.spacingLg))
        }
    }
}

/**
 * A selectable card showing the theme name, description, and a color swatch.
 */
@Composable
private fun ThemeSelectionCard(
    preset: ThemePreset,
    onClick: () -> Unit
) {
    val dimensions = Auth0Dimensions.default()
    val shapes = Auth0Shapes.default()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch showing the preset's primary + background colors
            ThemeColorSwatch(preset)

            Spacer(modifier = Modifier.width(dimensions.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A small visual swatch representing the theme's key colors or shape style.
 */
@Composable
private fun ThemeColorSwatch(preset: ThemePreset) {
    val colors = preset.configuration.color
    val primaryColor = colors?.backgroundPrimary ?: MaterialTheme.colorScheme.primary
    val bgColor = colors?.backgroundLayerBase ?: MaterialTheme.colorScheme.background

    val shapes = Auth0Shapes.default()
    val shape = if (preset is ThemePreset.BoldShapes) {
        shapes.large
    } else if (preset is ThemePreset.CompactShapes) {
        shapes.extraSmall
    } else {
        shapes.medium
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(primaryColor)
        )
    }
}
