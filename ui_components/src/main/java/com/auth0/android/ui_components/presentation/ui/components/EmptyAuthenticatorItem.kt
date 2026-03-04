package com.auth0.android.ui_components.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auth0.android.ui_components.theme.Auth0Theme


@Composable
fun EmptyAuthenticatorItem(
    modifier: Modifier = Modifier,
    emptyMessage: String
) {
    // Access theme tokens
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val shapes = Auth0Theme.shapes
    val sizes = Auth0Theme.sizes

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.inputHeight),
            shape = shapes.large,
            color = colors.backgroundLayerMedium,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = sizes.paddingLarge, horizontal = sizes.padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = typography.body,
                    color = colors.textDefault
                )
            }
        }
    }
}