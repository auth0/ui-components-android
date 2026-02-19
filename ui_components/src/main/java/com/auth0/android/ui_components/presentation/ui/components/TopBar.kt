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
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.Auth0TokenDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    topBarColor: Color = Color.White,
    showSeparator: Boolean = false,
    showBackNavigation: Boolean = true,
    trailingIcon: Painter? = null,
    titleTextStyle: TextStyle? = null,
    onBackClick: () -> Unit,
    trailingIconClick: () -> Unit = {}
) {
    val colors = Auth0TokenDefaults.color()
    val typography = Auth0TokenDefaults.typography()

    val titleStyle = titleTextStyle ?: typography.title

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = titleStyle,
                    color = colors.textPrimary
                )
            },
            navigationIcon = {
                if (showBackNavigation) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
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
                containerColor = topBarColor,
            ),
            modifier = modifier
        )
        if (showSeparator) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.3.dp,
                color = colors.border
            )
        }
    }
}