package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaForegroundEffect
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaSecureWindow
import com.auth0.universalcomponents.feature.mfachallenge.presentation.isShowingNewRecoveryCode
import com.auth0.universalcomponents.feature.mfachallenge.presentation.mirror
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents
import com.auth0.universalcomponents.presentation.ui.utils.stepTransition

/**
 * Draws the ViewModel's step, animates screen changes and owns the navigation bar and the one Back handler. During MFA
 * it places [MfaHost] inside its frame (unless the app draws MFA itself, headless). It only draws: moving into and out
 * of MFA is the ViewModel's, so nothing depends on a screen being on show. [onSignUp], when the app offers sign-up,
 * opens it from the link on the first screen.
 */
@Composable
internal fun LoginRenderingHost(
    viewModel: LoginFlowViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onMfaError: (MfaError) -> MfaErrorHandling = { MfaErrorHandling.NotHandled },
    mfa: MfaConfiguration = MfaConfiguration(),
    onSignUp: (() -> Unit)? = null,
) {
    val step by viewModel.loginState.collectAsStateWithLifecycle()
    val engine = (step as? LoginState.Mfa)?.engine
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnMfaError by rememberUpdatedState(onMfaError)

    LaunchedEffect(viewModel) { viewModel.send(LoginIntent.Start) }
    // Keyed on the ViewModel: a new sign-in (another connection, preference or sign-up setting) has events of its own.
    ObserveAsEvents(viewModel.events, key1 = viewModel) { event ->
        when (event) {
            is LoginEvent.Authenticated -> currentOnAuthenticated(event.credentials)
            is LoginEvent.MfaFailed -> viewModel.send(LoginIntent.MfaErrorAnswered(currentOnMfaError(event.error)))
        }
    }

    MfaForegroundEffect(engine)
    (mfa.presentation as? MfaPresentation.Headless)?.let { PublishToStream(it.stream, viewModel, engine) }

    val onBack: () -> Unit = remember(viewModel) { { if (!viewModel.goBack()) currentOnExit() } }
    BackHandler(onBack = onBack)

    val currentOnSignUp by rememberUpdatedState(onSignUp)
    val offersSignUp = onSignUp != null
    val context = remember(viewModel, mfa, offersSignUp) {
        RenderContext(
            dispatch = viewModel::send,
            back = onBack,
            mfa = mfa,
            typedPassword = viewModel.typedPassword,
            onSignUp = if (offersSignUp) ({ currentOnSignUp?.invoke() }) else null,
        )
    }
    // The typed password goes with its screen. A rotation draws the same screen again, so it keeps it.
    val isOnPassword = step is LoginState.CollectPassword
    LaunchedEffect(isOnPassword) { if (!isOnPassword) viewModel.typedPassword.value = "" }

    // The new recovery code is a live secret, so the window is kept out of screenshots while it is on show. Here and not
    // in MfaHost, so a headless app, which draws no MfaHost, is covered too.
    if (engine.isShowingNewRecoveryCode()) MfaSecureWindow()
    LoginScreenFrame(onBack = onBack, modifier = modifier, isBackEnabled = !step.isBusy) {
        AnimatedContent(
            targetState = plan(step),
            contentKey = { it.transitionIdentity },
            transitionSpec = {
                if (initialState.isSiblingOf(targetState)) {
                    // The code and the password of one person swap in place, rather than pushing one another aside.
                    fadeIn() togetherWith fadeOut()
                } else {
                    stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
                }
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
private fun PublishToStream(stream: MfaStepStream, viewModel: LoginFlowViewModel, engine: MfaEngine?) {
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
