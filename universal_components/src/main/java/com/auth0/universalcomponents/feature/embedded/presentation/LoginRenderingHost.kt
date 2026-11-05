package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenFrame
import com.auth0.universalcomponents.feature.mfachallenge.MfaConfiguration
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorHandling
import com.auth0.universalcomponents.feature.mfachallenge.MfaPresentation
import com.auth0.universalcomponents.feature.mfachallenge.MfaStepStream
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaForegroundEffect
import com.auth0.universalcomponents.feature.mfachallenge.presentation.mirror
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents
import com.auth0.universalcomponents.presentation.ui.utils.stepTransition

/**
 * Draws the ViewModel's step, animates screen changes and owns the navigation bar and the one Back handler. During MFA
 * it places [MfaHost] inside its frame (unless the app draws MFA itself, headless). It only draws: moving into and out
 * of MFA is the ViewModel's, so nothing depends on a screen being on show.
 */
@Composable
internal fun LoginRenderingHost(
    viewModel: LoginFlowViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onMfaError: (MfaError) -> MfaErrorHandling = { MfaErrorHandling.NotHandled },
    mfa: MfaConfiguration = MfaConfiguration(),
) {
    val step by viewModel.loginState.collectAsStateWithLifecycle()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnMfaError by rememberUpdatedState(onMfaError)

    LaunchedEffect(viewModel) { viewModel.send(LoginIntent.Start) }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LoginEvent.Authenticated -> currentOnAuthenticated(event.credentials)
            is LoginEvent.MfaFailed -> viewModel.send(LoginIntent.MfaErrorAnswered(currentOnMfaError(event.error)))
        }
    }

    MfaForegroundEffect((step as? LoginState.Mfa)?.engine)
    (mfa.presentation as? MfaPresentation.Headless)?.let { PublishToStream(it.stream, viewModel, step) }

    val onBack: () -> Unit = remember(viewModel) { { if (!viewModel.goBack()) currentOnExit() } }
    BackHandler(onBack = onBack)

    val context = remember(viewModel, mfa) { RenderContext(dispatch = viewModel::send, back = onBack, mfa = mfa) }

    LoginScreenFrame(onBack = onBack, modifier = modifier) {
        AnimatedContent(
            targetState = plan(step),
            contentKey = { it.transitionIdentity },
            transitionSpec = {
                stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
            },
            modifier = Modifier.fillMaxSize(),
            label = "loginState",
        ) { plan ->
            LoginRenderer(plan, context)
        }
    }
}

/**
 * Mirrors MFA into a headless app's [stream]: the step to draw while MFA runs (null otherwise), and how it ended.
 * The step is cleared when this leaves the composition, so a stale step is never left behind.
 */
@Composable
private fun PublishToStream(stream: MfaStepStream, viewModel: LoginFlowViewModel, step: LoginState) {
    val engine = (step as? LoginState.Mfa)?.engine
    LaunchedEffect(stream, engine) {
        if (engine == null) {
            stream.showStep(null)
        } else {
            stream.mirror(engine, onLeave = { viewModel.send(LoginIntent.LeaveMfa) })
        }
    }
    LaunchedEffect(stream, viewModel) { viewModel.mfaResult.collect(stream::finish) }
    DisposableEffect(stream) { onDispose { stream.showStep(null) } }
}
