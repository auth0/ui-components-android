package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auth0.universalcomponents.feature.embedded.LocalEmbeddedEntry
import com.auth0.universalcomponents.feature.embedded.titleOr
import com.auth0.universalcomponents.presentation.ui.flow.FlowScreenHeader
import com.auth0.universalcomponents.presentation.ui.flow.FlowStepLayout
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * What the email, phone and username screens put on show: the [field] and the Continue button, with the [alternatives]
 * (the "OR" and the other ways to sign in) when there are any.
 *
 * On the first screen ([isEntry]) they make up the entry screen, with its logo and centered title, and the app's title if
 * it set one. A way opened from "Continue with …" is a plain screen: a title above the field, and Continue at the bottom.
 */
@Composable
internal fun EntryStepLayout(
    isEntry: Boolean,
    title: String,
    field: @Composable () -> Unit,
    continueButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    alternatives: (@Composable () -> Unit)? = null,
) {
    if (isEntry) {
        EmbeddedEntryScaffold(
            title = LocalEmbeddedEntry.current.messages.titleOr(title),
            modifier = modifier,
            form = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    field()
                    Spacer(Modifier.height(Auth0Theme.dimensions.spacingLg))
                    continueButton()
                }
            },
            alternatives = alternatives,
        )
    } else {
        FlowStepLayout(
            modifier = modifier,
            pageDescription = { FlowScreenHeader(title = title) },
            content = field,
            primaryAction = continueButton,
            secondaryAction = alternatives,
            // Next to Continue, as in the design with other methods; alone, Continue stays at the bottom.
            pinActionsToBottom = alternatives == null,
        )
    }
}
