package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * Places the Figma step slots: description, content, helper text, then the actions at the bottom or right below.
 * Everything scrolls when it doesn't fit between the navigation bar and the keyboard, as in landscape;
 * when it fits, pinned actions still sit at the bottom.
 */
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(dimensions.spacingLg),
            verticalArrangement = if (pinActionsToBottom) Arrangement.SpaceBetween else Arrangement.Top,
        ) {
            Column {
                pageDescription()
                content()
                if (helperText != null) {
                    Spacer(Modifier.height(dimensions.spacingMd))
                    helperText()
                }
            }
            Column {
                Spacer(Modifier.height(dimensions.spacingLg))
                primaryAction()
                if (secondaryAction != null) {
                    Spacer(Modifier.height(dimensions.spacingLg))
                    secondaryAction()
                }
            }
        }
    }
}
