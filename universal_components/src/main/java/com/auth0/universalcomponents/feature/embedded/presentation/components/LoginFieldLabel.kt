package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auth0.universalcomponents.theme.Auth0Theme

/** The label above a field, followed by the gap to the field. */
@Composable
internal fun LoginFieldLabel(text: String) {
    Text(text = text, style = Auth0Theme.typography.label, color = Auth0Theme.colors.textBold)
    Spacer(Modifier.height(Auth0Theme.dimensions.spacingMd))
}
