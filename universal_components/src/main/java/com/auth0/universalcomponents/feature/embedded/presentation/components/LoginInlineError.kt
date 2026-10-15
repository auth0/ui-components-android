package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auth0.universalcomponents.theme.Auth0Theme

/** The error message shown under a field. */
@Composable
internal fun LoginInlineError(message: String) {
    Text(
        text = message,
        style = Auth0Theme.typography.bodySmall,
        color = Auth0Theme.colors.textOnError,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Auth0Theme.dimensions.spacingXs),
    )
}
