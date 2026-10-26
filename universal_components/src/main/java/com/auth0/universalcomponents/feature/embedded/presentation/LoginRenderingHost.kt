package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.di.UniversalComponentsModule
import com.auth0.universalcomponents.feature.embedded.di.EmbeddedAuthSession
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenFrame
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaFlowHost
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaViewModel
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents

private const val TRANSITION_MILLIS = 280

/**
 * Draws the ViewModel's step, animates screen changes and owns the navigation bar.
 * On a second factor it hands the screen to the MFA flow, on the same [session] as the first factor.
 */
@Composable
internal fun LoginRenderingHost(
    viewModel: LoginFlowViewModel,
    session: EmbeddedAuthSession,
    connection: String,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step by viewModel.loginStep.collectAsState()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnExit by rememberUpdatedState(onExit)

    LaunchedEffect(viewModel) { viewModel.send(LoginEvent.Start) }
    ObserveAsEvents(viewModel.authenticated) { currentOnAuthenticated(it) }

    val current = step
    if (current is LoginStep.Mfa) {
        // One MFA ViewModel per visit, so going back to email and through MFA again starts it fresh.
        val mfaViewModel: MfaViewModel = viewModel(
            key = "embedded-mfa:$connection:${current.attempt}",
            factory = UniversalComponentsModule.provideMfaViewModelFactory(session.client, current.factors),
        )
        MfaFlowHost(
            viewModel = mfaViewModel,
            onAuthenticated = { viewModel.send(LoginEvent.MfaSignedIn(it)) },
            onRestart = { viewModel.send(LoginEvent.LeaveMfa) },
            modifier = modifier,
        )
        return
    }

    val onBack: () -> Unit = remember(viewModel) { { if (!viewModel.goBack()) currentOnExit() } }
    BackHandler(onBack = onBack)

    val context = remember(viewModel) { RenderContext(dispatch = viewModel::send, back = onBack) }

    LoginScreenFrame(onBack = onBack, modifier = modifier) {
        AnimatedContent(
            targetState = plan(current),
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

/** A deeper screen pushes in from the end; a shallower one pops back in from the start. */
internal fun stepTransition(isForward: Boolean): ContentTransform {
    val direction = if (isForward) 1 else -1
    val spec = tween<Float>(TRANSITION_MILLIS, easing = EaseInOut)
    val slide = tween<IntOffset>(TRANSITION_MILLIS, easing = EaseInOut)
    return (slideInHorizontally(slide) { direction * it } + fadeIn(spec)) togetherWith
        (slideOutHorizontally(slide) { -direction * it } + fadeOut(spec))
}
