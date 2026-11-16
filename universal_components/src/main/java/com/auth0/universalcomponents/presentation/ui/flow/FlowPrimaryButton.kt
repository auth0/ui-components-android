package com.auth0.universalcomponents.presentation.ui.flow

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.auth0.universalcomponents.presentation.ui.components.GradientButton
import com.auth0.universalcomponents.theme.Auth0Theme

/** The main call-to-action, drawn with the library's shared button. It takes the focus (and the keyboard) away unless [clearsFocus] is false. */
@Composable
internal fun FlowPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    clearsFocus: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    GradientButton(
        modifier = modifier
            .fillMaxWidth()
            .height(Auth0Theme.sizes.buttonHeight),
        isLoading = isLoading,
        enabled = enabled,
        onClick = {
            if (clearsFocus) focusManager.clearFocus()
            onClick()
        },
    ) {
        Text(text = title, style = Auth0Theme.typography.label)
    }
}
