package com.auth0.universalcomponents.presentation.ui.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.theme.Auth0Theme

/**
 * A quiet wait: a small spinner that fades in, so a fast load doesn't flicker. When [showAtOnce], the spinner is
 * there from the first frame: for a wait the user caused and is sure to notice, such as a push going out, where a
 * blank screen would look broken.
 */
@Composable
internal fun FlowLoadingScreen(modifier: Modifier = Modifier, showAtOnce: Boolean = false) {
    var showLoader by remember { mutableStateOf(showAtOnce) }
    LaunchedEffect(Unit) {
        showLoader = true
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = showLoader, enter = fadeIn(), exit = fadeOut()) {
            CircularLoader(modifier = Modifier.size(Auth0Theme.sizes.iconLarge))
        }
    }
}

/**
 * The flow stopped; "Try again" picks up from the last screen. An empty [message] shows a generic one. MFA passes its
 * own [title] and "Start over" as [actionTitle].
 */
@Composable
internal fun FlowErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "",
    title: String = stringResource(R.string.embedded_login_error_title),
    actionTitle: String = stringResource(R.string.try_again),
) {
    FlowStatusScreen(
        title = title,
        description = message.ifEmpty { stringResource(R.string.embedded_login_error_fallback) },
        actionTitle = actionTitle,
        onAction = onRetry,
        modifier = modifier,
    )
}

/**
 * A title and optional description with an optional button right below, centred together on the screen.
 * It scrolls when it doesn't fit, as in landscape with large text.
 */
@Composable
internal fun FlowStatusScreen(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensions.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMd),
        ) {
            Text(
                text = title,
                style = typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = colors.textBold,
                textAlign = TextAlign.Center,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = typography.bodySmall,
                    color = colors.textDefault,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionTitle != null && onAction != null) {
                FlowPrimaryButton(title = actionTitle, onClick = onAction)
            }
        }
    }
}
