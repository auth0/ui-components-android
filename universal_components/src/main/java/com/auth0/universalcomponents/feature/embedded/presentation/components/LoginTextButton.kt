package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.auth0.universalcomponents.theme.Auth0Theme

/** A centred text action under the main button, like Resend. */
@Composable
internal fun LoginTextButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Auth0Theme.colors
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            style = Auth0Theme.typography.body,
            color = if (enabled) colors.textBold else colors.textDisabled,
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        )
    }
}
