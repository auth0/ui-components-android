package com.auth0.universalcomponents.feature.embedded.presentation.screens

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
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginPrimaryButton
import com.auth0.universalcomponents.presentation.ui.components.CircularLoader
import com.auth0.universalcomponents.theme.Auth0Theme

/** A quiet wait: a small spinner that fades in. */
@Composable
internal fun LoginLoadingScreen(modifier: Modifier = Modifier) {
    var showLoader by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showLoader = true
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = showLoader, enter = fadeIn(), exit = fadeOut()) {
            CircularLoader(modifier = Modifier.size(Auth0Theme.sizes.iconLarge))
        }
    }
}

/** The flow stopped; "Try again" picks up from the last screen. An empty [message] shows a generic one. */
@Composable
internal fun LoginErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "",
) {
    LoginStatusScreen(
        title = stringResource(R.string.embedded_login_error_title),
        description = message.ifEmpty { stringResource(R.string.embedded_login_error_fallback) },
        actionTitle = stringResource(R.string.try_again),
        onAction = onRetry,
        modifier = modifier,
    )
}

/** Shown briefly before the app moves on. */
@Composable
internal fun LoginSuccessScreen(modifier: Modifier = Modifier) {
    LoginStatusScreen(title = stringResource(R.string.embedded_login_success_title), modifier = modifier)
}

/** The server offered nothing this flow can drive. A null [onBack] hides "Go back". */
@Composable
internal fun LoginNoMethodsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    LoginStatusScreen(
        title = stringResource(R.string.embedded_login_no_methods_title),
        description = stringResource(R.string.embedded_login_no_methods_description),
        actionTitle = stringResource(R.string.embedded_login_go_back),
        onAction = onBack,
        modifier = modifier,
    )
}

/**
 * A title and optional description with an optional button right below, centred together on the screen.
 * It scrolls when it doesn't fit, as in landscape with large text.
 */
@Composable
private fun LoginStatusScreen(
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
                LoginPrimaryButton(title = actionTitle, onClick = onAction)
            }
        }
    }
}
