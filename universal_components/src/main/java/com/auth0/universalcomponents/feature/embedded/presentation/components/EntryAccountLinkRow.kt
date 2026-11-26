package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.auth0.universalcomponents.theme.Auth0Theme

/** The text size from which the prompt and the action stack, as they would not fit side by side. */
private const val STACKED_FONT_SCALE = 1.5f

private const val DISABLED_ALPHA = 0.5f

/**
 * "Don't have an account? Sign up": the [EntryLink.prompt], then its action as a button in [color], or the theme's
 * link color when that is null. At the largest text sizes they stack. The action is dimmed and does nothing while
 * [enabled] is false, as when a request is on its way. Compose widens its touch area to 48dp without adding space.
 */
@Composable
internal fun EntryAccountLinkRow(
    link: EntryLink,
    color: Color?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val typography = Auth0Theme.typography
    val prompt: @Composable () -> Unit = {
        Text(
            text = link.prompt,
            style = typography.helper,
            color = Auth0Theme.colors.textDefault,
            textAlign = TextAlign.Center,
        )
    }
    val action: @Composable () -> Unit = {
        Text(
            text = link.action,
            style = typography.helper,
            color = color ?: Auth0Theme.colors.foregroundLinkPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clickable(enabled = enabled, role = Role.Button, onClick = link.onClick),
        )
    }
    if (LocalDensity.current.fontScale >= STACKED_FONT_SCALE) {
        Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            prompt()
            action()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Auth0Theme.dimensions.spacingXs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            prompt()
            action()
        }
    }
}
