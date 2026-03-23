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
import com.auth0.android.sample.ui.viewmodels.AppearanceViewModel
import com.auth0.universalcomponents.presentation.ui.components.GradientButton
import com.auth0.universalcomponents.presentation.ui.components.TopBar
import com.auth0.android.sample.ui.theme.isAuth0DarkTheme
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Appearance/Theme selection screen.
 *
 * Shows radio buttons for: Automatic, Light, Dark.
 * The "Update Theme" button applies the selected theme globally and navigates back.
 *
 * @param onBack Navigate back
 * @param appearanceViewModel Shared ViewModel that holds the global theme state
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    appearanceViewModel: AppearanceViewModel
) {
    val selectedIndex by appearanceViewModel.selectedIndex.collectAsStateWithLifecycle()
    val previewOption = appearanceViewModel.themeOptions.getOrElse(selectedIndex) {
        appearanceViewModel.themeOptions[0]
    }

    // Wrap in a local Auth0Theme so the screen previews the selected theme immediately,
    // while the global theme (in MainActivity) only updates when "Update Theme" is tapped.
    Auth0Theme(
        configuration = previewOption.configuration,
        darkTheme = previewOption.darkTheme
    ) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    val isDark = isAuth0DarkTheme()

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
                                selectedColor = if (isDark) colors.backgroundAccent else colors.backgroundPrimary,
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

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight),
                onClick = {
                    appearanceViewModel.applySelectedTheme()
                    onBack()
                }
            ) {
                Text(
                    text = "Update Theme",
                    style = typography.label,
                    color = colors.textOnPrimary
                )
            }

        }
    }
    } // Auth0Theme
}
