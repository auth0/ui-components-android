package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaFactorListScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaPushScreen
import com.auth0.universalcomponents.feature.embedded.presentation.stepTransition
import com.auth0.universalcomponents.feature.embedded.presentation.text

/**
 * Draws [engine]'s steps and sends every tap straight back to it, so whoever hosts MFA needs to know nothing of its
 * screens or taps. It draws no frame of its own: the host places it inside its frame (embedded login's), so moving
 * into MFA animates like any other step, and moving between MFA screens animates here.
 */
@Composable
internal fun MfaHost(engine: MfaEngine, modifier: Modifier = Modifier) {
    val step by engine.step.collectAsStateWithLifecycle()
    AnimatedContent(
        targetState = step,
        contentKey = { it.transitionIdentity },
        transitionSpec = {
            stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
        },
        modifier = modifier.fillMaxSize(),
        label = "mfaStep",
    ) { shown ->
        MfaScreen(shown, onIntent = engine::onIntent)
    }
}

/** Draws one [MfaStep] and sends every tap back as an [MfaIntent]. It holds no logic. */
@Composable
private fun MfaScreen(step: MfaStep, onIntent: (MfaIntent) -> Unit) {
    when (step) {
        is MfaStep.AuthenticatorList -> MfaFactorListScreen(
            factors = step.factors,
            onSelect = { onIntent(MfaIntent.SelectFactor(it)) },
        )

        is MfaStep.PushWaiting -> {
            val phase = step.phase
            MfaPushScreen(
                deviceName = step.factor.deviceName,
                resendSecondsLeft = (phase as? PushPhase.Polling)?.resendSecondsLeft ?: 0,
                onResend = { onIntent(MfaIntent.Resend) },
                isSending = phase == PushPhase.Sending,
                onChooseAnotherDevice = { onIntent(MfaIntent.ChooseAnotherFactor) }.takeIf { step.canChooseAnother },
            )
        }

        is MfaStep.Failed -> LoginErrorScreen(
            message = step.error.mfaText(),
            onRetry = { onIntent(MfaIntent.Retry) },
        )
    }
}

/** Names the screen and ignores its data, so a data-only change doesn't animate. */
private val MfaStep.transitionIdentity: String
    get() = when (this) {
        is MfaStep.AuthenticatorList -> "list"
        is MfaStep.PushWaiting -> "push"
        is MfaStep.Failed -> "failed"
    }

/** Compared across a change: deeper pushes, shallower pops. */
private val MfaStep.navigationDepth: Int
    get() = when (this) {
        is MfaStep.AuthenticatorList -> 0
        is MfaStep.PushWaiting -> 1
        is MfaStep.Failed -> 2
    }

/** The login wording, except where it assumes an emailed first-factor code. */
@Composable
private fun EmbeddedLoginError.mfaText(): String = when (this) {
    EmbeddedLoginError.CodeExpired -> stringResource(R.string.embedded_login_mfa_error_expired)
    else -> text()
}
