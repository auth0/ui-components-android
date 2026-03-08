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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.sample.ui.viewmodels.AppearanceViewModel
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Appearance/Theme selection screen matching the Figma design.
 *
 * Shows radio buttons for: Automatic, Light, Dark, Custom Theme (Olive), Custom Theme (Purple).
 * The "Update Theme" button navigates to Settings with the selected theme applied.
 *
 * @param onBack Navigate back
 * @param onUpdateTheme Callback with the selected ThemePreset index
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    onUpdateTheme: (Int) -> Unit,
    appearanceViewModel: AppearanceViewModel = viewModel()
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes

    val selectedIndex by appearanceViewModel.selectedIndex.collectAsStateWithLifecycle()

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
                .padding(horizontal = dimensions.spacingLg)
        ) {
            Spacer(modifier = Modifier.height(dimensions.spacingMd))

            Text(
                text = "Appearance",
                style = typography.displayMedium,
                color = colors.textBold
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            Text(
                text = "THEME",
                style = typography.helper,
                color = colors.textDefault
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXs))

            // Theme options list in a card-like container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colors.backgroundLayerTop,
                        shape = shapes.large
                    )
            ) {
                appearanceViewModel.themeOptions.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appearanceViewModel.selectTheme(index) }
                            .padding(
                                horizontal = dimensions.spacingLg,
                                vertical = dimensions.spacingMd
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = typography.body,
                            color = colors.textBold,
                            modifier = Modifier.weight(1f)
                        )

                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = { appearanceViewModel.selectTheme(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.backgroundPrimary,
                                unselectedColor = colors.borderDefault
                            )
                        )
                    }

                    if (index < appearanceViewModel.themeOptions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = dimensions.spacingLg),
                            thickness = 0.5.dp,
                            color = colors.borderSubtle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight),
                onClick = {
                    onUpdateTheme(appearanceViewModel.getSelectedPresetIndex())
                }
            ) {
                Text(
                    text = "Update Theme",
                    style = typography.label,
                    color = colors.textOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacingLg))
        }
    }
}
