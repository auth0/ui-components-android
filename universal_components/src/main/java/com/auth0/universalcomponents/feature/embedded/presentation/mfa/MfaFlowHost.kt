package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.presentation.components.LoginScreenFrame
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginLoadingScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaFactorListScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaPushScreen
import com.auth0.universalcomponents.feature.embedded.presentation.stepTransition
import com.auth0.universalcomponents.feature.embedded.presentation.text
import com.auth0.universalcomponents.presentation.ui.utils.ObserveAsEvents

/**
 * Draws the MFA flow inside the same frame as login and sends every tap to [viewModel] as an intent.
 * [onAuthenticated] receives the credentials; [onRestart] means the sign-in starts over from the first factor.
 */
@Composable
internal fun MfaFlowHost(
    viewModel: MfaViewModel,
    onAuthenticated: (Credentials) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnRestart by rememberUpdatedState(onRestart)

    LaunchedEffect(viewModel) { viewModel.onIntent(MfaIntent.Start) }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is MfaEffect.Authenticated -> currentOnAuthenticated(effect.credentials)
            MfaEffect.Restart -> currentOnRestart()
        }
    }

    val onBack = { viewModel.onIntent(MfaIntent.Back) }
    BackHandler(onBack = onBack)

    LoginScreenFrame(onBack = onBack, modifier = modifier) {
        AnimatedContent(
            targetState = state.step,
            contentKey = { it.identity },
            transitionSpec = { stepTransition(isForward = targetState.depth >= initialState.depth) },
            modifier = Modifier.fillMaxSize(),
            label = "mfaStep",
        ) { step ->
            MfaStepScreen(step, factors = state.factors, onIntent = viewModel::onIntent)
        }
    }
}

@Composable
private fun MfaStepScreen(step: MfaStep, factors: List<MfaFactor>, onIntent: (MfaIntent) -> Unit) {
    when (step) {
        MfaStep.Loading -> LoginLoadingScreen()

        MfaStep.ChooseFactor -> MfaFactorListScreen(
            factors = factors,
            onSelect = { onIntent(MfaIntent.SelectFactor(it)) },
        )

        is MfaStep.WaitForPush -> MfaPushScreen(
            deviceName = step.factor.deviceName,
            isSending = step.isSending,
            onUseAnotherMethod = if (factors.size > 1) ({ onIntent(MfaIntent.Back) }) else null,
        )

        is MfaStep.Failed -> LoginErrorScreen(
            message = step.error.mfaText(),
            onRetry = { onIntent(MfaIntent.Retry) },
        )

        MfaStep.Authenticated -> LoginSuccessScreen()
    }
}

/** The login wording, except where it assumes an emailed first-factor code. */
@Composable
private fun EmbeddedLoginError.mfaText(): String = when (this) {
    EmbeddedLoginError.CodeExpired -> stringResource(R.string.embedded_login_mfa_error_expired)
    else -> text()
}

/** Names the screen and ignores its data, so a data-only change doesn't animate. */
private val MfaStep.identity: String
    get() = when (this) {
        MfaStep.Loading -> "loading"
        MfaStep.ChooseFactor -> "chooseFactor"
        is MfaStep.WaitForPush -> "push"
        is MfaStep.Failed -> "failed"
        MfaStep.Authenticated -> "authenticated"
    }

/** Compared across a change: deeper pushes, shallower pops. */
private val MfaStep.depth: Int
    get() = when (this) {
        MfaStep.Loading, MfaStep.ChooseFactor -> 0
        is MfaStep.WaitForPush -> 1
        is MfaStep.Failed, MfaStep.Authenticated -> 2
    }
