package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
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
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenFrame
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents

private const val TRANSITION_MILLIS = 280

/** Draws the ViewModel's step, animates screen changes and owns the navigation bar. */
@Composable
internal fun LoginRenderingHost(
    viewModel: LoginFlowViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step by viewModel.loginStep.collectAsState()
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
            transitionSpec = { pushTransition() },
            modifier = Modifier.fillMaxSize(),
            label = "loginStep",
        ) { plan ->
            LoginRenderer(plan, context)
        }
    }
}

/** A deeper screen pushes in from the end; a shallower one pops back in from the start. */
private fun AnimatedContentTransitionScope<ScreenPlan>.pushTransition(): ContentTransform {
    val direction = if (targetState.navigationDepth >= initialState.navigationDepth) 1 else -1
    val spec = tween<Float>(TRANSITION_MILLIS, easing = EaseInOut)
    val slide = tween<IntOffset>(TRANSITION_MILLIS, easing = EaseInOut)
    return (slideInHorizontally(slide) { direction * it } + fadeIn(spec)) togetherWith
        (slideOutHorizontally(slide) { -direction * it } + fadeOut(spec))
}
