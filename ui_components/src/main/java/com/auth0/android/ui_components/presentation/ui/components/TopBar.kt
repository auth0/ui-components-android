package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import com.auth0.android.ui_components.styles.components.TopBarStyle
import com.auth0.android.ui_components.theme.Auth0Theme
import com.auth0.android.ui_components.theme.SeparatorLineGray

/**
 * Top app bar component with theme support.
 *
 * @param title The title text to display
 * @param modifier Modifier for the top bar
 * @param style Optional TopBarStyle for customization. When provided, overrides individual style parameters.
 * @param topBarColor Background color (used when style is not provided)
 * @param showSeparator Whether to show a divider below the top bar (used when style is not provided)
 * @param showBackNavigation Whether to show the back navigation icon
 * @param trailingIcon Optional trailing icon
 * @param titleTextStyle Text style for the title (used when style is not provided)
 * @param onBackClick Callback when back button is clicked
 * @param trailingIconClick Callback when trailing icon is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    style: TopBarStyle = TopBarStyle.Default,
    topBarColor: Color? = null,
    showSeparator: Boolean? = null,
    showBackNavigation: Boolean = true,
    trailingIcon: Painter? = null,
    titleTextStyle: TextStyle? = null,
    onBackClick: () -> Unit,
    trailingIconClick: () -> Unit = {}
) {
    val backgroundColor = style.backgroundColor

    val showDivider = style.showDivider

    val dividerThickness = Auth0Theme.dimensions.dividerThickness

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = style.titleStyle,
                )
            },
            navigationIcon = {
                if (showBackNavigation) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint =  MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = {
                // Show trailing icon if provided
                if (trailingIcon != null) {
                    IconButton(onClick = trailingIconClick) {
                        Icon(
                            painter = trailingIcon,
                            contentDescription = "Action",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = backgroundColor,
            ),
            modifier = modifier
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = dividerThickness,
                color = SeparatorLineGray
            )
        }
    }
}