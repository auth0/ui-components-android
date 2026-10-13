package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.auth0.universalcomponents.presentation.ui.components.GradientButton
import com.auth0.universalcomponents.theme.Auth0Theme

/** The main call-to-action, drawn with the library's shared button. */
@Composable
internal fun LoginPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    GradientButton(
        modifier = modifier
            .fillMaxWidth()
            .height(Auth0Theme.sizes.buttonHeight),
        isLoading = isLoading,
        enabled = enabled,
        onClick = {
            focusManager.clearFocus()
            onClick()
        },
    ) {
        Text(text = title, style = Auth0Theme.typography.label)
    }
}
