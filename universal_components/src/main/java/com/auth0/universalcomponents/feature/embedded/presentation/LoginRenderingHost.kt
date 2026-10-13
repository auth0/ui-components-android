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
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginEvent
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginStep
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
    val step by viewModel.step.collectAsState()
    val inputError by viewModel.inputError.collectAsState()
    val isResending by viewModel.isResending.collectAsState()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnExit by rememberUpdatedState(onExit)

    LaunchedEffect(viewModel) { viewModel.send(LoginEvent.Start) }
    ObserveAsEvents(viewModel.authenticated) { currentOnAuthenticated(it) }

    // Kept on screen, with a spinner in its button, while the ViewModel is loading mid-flow.
    val lastInput = remember { LastInputStep() }
    lastInput.track(step)
    val displayStep = if (step == LoginStep.Loading) lastInput.step ?: step else step
    val isSubmitting = step == LoginStep.Loading && lastInput.step != null

    val onBack: () -> Unit = remember(viewModel) { { if (!viewModel.goBack()) currentOnExit() } }
    BackHandler(onBack = onBack)

    val context = RenderContext(
        isSubmitting = isSubmitting,
        isResending = isResending,
        inputError = inputError,
        dispatch = viewModel::send,
        resend = viewModel::resend,
        back = onBack,
    )

    LoginScreenFrame(onBack = onBack, modifier = modifier) {
        AnimatedContent(
            targetState = plan(displayStep),
            contentKey = { it.transitionIdentity },
            transitionSpec = { pushTransition() },
            modifier = Modifier.fillMaxSize(),
            label = "loginStep",
        ) { plan ->
            LoginRenderer(plan, context)
        }
    }
}

/** The last input step seen; plain state, read in the same pass that updates it. */
private class LastInputStep {

    var step: LoginStep? = null
        private set

    fun track(current: LoginStep) {
        when (current) {
            is LoginStep.CollectOtpIdentifier, is LoginStep.CollectOtpCode, is LoginStep.StepUpMfa -> step = current
            LoginStep.Loading -> Unit
            else -> step = null
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
