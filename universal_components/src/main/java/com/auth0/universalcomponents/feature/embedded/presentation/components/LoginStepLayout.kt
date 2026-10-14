package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auth0.universalcomponents.theme.Auth0Theme

/** Places the Figma step slots: description, content, helper text, then the actions at the bottom or right below. */
@Composable
internal fun LoginStepLayout(
    pageDescription: @Composable () -> Unit,
    content: @Composable () -> Unit,
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    helperText: (@Composable () -> Unit)? = null,
    secondaryAction: (@Composable () -> Unit)? = null,
    pinActionsToBottom: Boolean = true,
) {
    val dimensions = Auth0Theme.dimensions
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(dimensions.spacingLg),
    ) {
        pageDescription()
        content()
        if (helperText != null) {
            Spacer(Modifier.height(dimensions.spacingMd))
            helperText()
        }
        if (pinActionsToBottom) {
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(dimensions.spacingLg))
        }
        primaryAction()
        if (secondaryAction != null) {
            Spacer(Modifier.height(dimensions.spacingLg))
            secondaryAction()
        }
    }
}
