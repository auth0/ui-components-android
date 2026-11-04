package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenFrame
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents
import com.auth0.universalcomponents.presentation.ui.utils.stepTransition

/**
 * Draws the ViewModel's step, animates screen changes and owns the navigation bar and the one Back handler. During MFA
 * it places [MfaHost] inside its frame. It only draws: moving into and out of MFA is the ViewModel's, so nothing
 * depends on a screen being on show.
 */
@Composable
internal fun LoginRenderingHost(
    viewModel: LoginFlowViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step by viewModel.loginStep.collectAsStateWithLifecycle()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnExit by rememberUpdatedState(onExit)

    LaunchedEffect(viewModel) { viewModel.send(LoginEvent.Start) }
    ObserveAsEvents(viewModel.authenticated) { currentOnAuthenticated(it) }

    val onBack: () -> Unit = remember(viewModel) { { if (!viewModel.goBack()) currentOnExit() } }
    BackHandler(onBack = onBack)

    val context = remember(viewModel) { RenderContext(dispatch = viewModel::send, back = onBack) }

    LoginScreenFrame(onBack = onBack, modifier = modifier) {
        AnimatedContent(
            targetState = plan(step),
            contentKey = { it.transitionIdentity },
            transitionSpec = {
                stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
            },
            modifier = Modifier.fillMaxSize(),
            label = "loginStep",
        ) { plan ->
            LoginRenderer(plan, context)
        }
    }
}
