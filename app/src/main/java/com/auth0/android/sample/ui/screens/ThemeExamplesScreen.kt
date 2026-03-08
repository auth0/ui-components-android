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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Select a Theme",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose a theme to apply to the Authenticator Settings component.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        itemsIndexed(ThemePreset.all()) { index, preset ->
            ThemeSelectionCard(
                preset = preset,
                onClick = { onThemeSelected(index) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch showing the preset's primary + background colors
            ThemeColorSwatch(preset)

            Spacer(modifier = Modifier.width(16.dp))

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

    val shape = if (preset is ThemePreset.BoldShapes) {
        RoundedCornerShape(16.dp)
    } else if (preset is ThemePreset.CompactShapes) {
        RoundedCornerShape(4.dp)
    } else {
        RoundedCornerShape(10.dp)
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
