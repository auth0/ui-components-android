package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaError
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginLoadingScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaFactorListScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaPushScreen
import com.auth0.universalcomponents.presentation.ui.utils.stepTransition

/**
 * Draws [engine]'s steps and sends every tap straight back to it, so whoever hosts MFA needs to know nothing of its
 * screens or taps. It draws no frame of its own: the host places it inside its frame (embedded login's), so moving
 * into MFA animates like any other step, and moving between MFA screens animates here.
 *
 * Once MFA has ended, its error screen is all that's left and the engine takes no more taps, so "Start over" calls
 * [onLeave]: the host decides where it goes (embedded login: back to email entry).
 */
@Composable
internal fun MfaHost(engine: MfaEngine, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    val step by engine.step.collectAsStateWithLifecycle()
    // The app's lifecycle, so a network failure while the user approves in another app doesn't count against the push.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { engine.onAppForegrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { engine.onAppBackgrounded() }
    AnimatedContent(
        targetState = step,
        contentKey = { it.transitionIdentity },
        transitionSpec = {
            stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
        },
        modifier = modifier.fillMaxSize(),
        label = "mfaStep",
    ) { shown ->
        MfaScreen(shown, onIntent = engine::onIntent, onLeave = onLeave)
    }
}

/** Draws one [MfaStep] and sends every tap back as an [MfaIntent]. It holds no logic. */
@Composable
private fun MfaScreen(step: MfaStep, onIntent: (MfaIntent) -> Unit, onLeave: () -> Unit) {
    when (step) {
        is MfaStep.ChooseFactor -> MfaFactorListScreen(
            factors = step.round.factors,
            onSelect = { onIntent(MfaIntent.SelectFactor(it)) },
            isAdditionalRound = step.round.isAdditional,
        )

        // The challenge goes out before its screen: over the list its row spins, otherwise a short loading shows.
        is MfaStep.SendingChallenge -> if (step.round.showsList) {
            MfaFactorListScreen(
                factors = step.round.factors,
                onSelect = {},
                isAdditionalRound = step.round.isAdditional,
                sending = step.factor,
            )
        } else {
            LoginLoadingScreen(showAtOnce = true)
        }

        is MfaStep.PushWaiting -> MfaPushScreen(
            deviceName = step.factor.deviceName,
            canResend = step.canResend,
            resendSecondsLeft = step.resendSecondsLeft,
            onResend = { onIntent(MfaIntent.Resend) },
            isResending = step.phase == PushPhase.Resending,
            onVerifyAnotherWay = { onIntent(MfaIntent.ChooseAnotherFactor) }.takeIf { step.round.canSwitch },
        )

        // As on iOS, an unexpected error shows the server's own words when it gave some.
        is MfaStep.Failed -> LoginErrorScreen(
            title = stringResource(step.error.text.title),
            message = (step.error as? MfaError.Unexpected)?.serverMessage
                ?: stringResource(step.error.text.description),
            actionTitle = stringResource(R.string.embedded_login_mfa_start_over),
            onRetry = onLeave,
        )
    }
}

/**
 * Names the screen and ignores its data, so a data-only change doesn't animate. A challenge going out keeps the list
 * it was picked from, and each round's list is a new screen.
 */
private val MfaStep.transitionIdentity: String
    get() = when (this) {
        is MfaStep.ChooseFactor -> "list:${round.number}"
        is MfaStep.SendingChallenge -> if (round.showsList) "list:${round.number}" else "sending:${round.number}"
        is MfaStep.PushWaiting -> "push:${round.number}"
        is MfaStep.Failed -> "failed"
    }

/** Compared across a change: deeper pushes, shallower pops. Each round goes deeper than the one before. */
private val MfaStep.navigationDepth: Int
    get() = when (this) {
        is MfaStep.ChooseFactor -> round.depth
        is MfaStep.SendingChallenge -> round.depth
        is MfaStep.PushWaiting -> round.depth + 1
        is MfaStep.Failed -> Int.MAX_VALUE
    }

private val MfaRound.depth: Int
    get() = number * 2

/** The error screen's words, the same as iOS. */
private class MfaErrorText(@StringRes val title: Int, @StringRes val description: Int)

private val MfaError.text: MfaErrorText
    get() = when (this) {
        is MfaError.Rejected -> MfaErrorText(
            R.string.embedded_login_mfa_error_rejected_title,
            R.string.embedded_login_mfa_error_rejected,
        )
        is MfaError.TokenExpired -> MfaErrorText(
            R.string.embedded_login_mfa_error_token_expired_title,
            R.string.embedded_login_mfa_error_token_expired,
        )
        is MfaError.TokenInvalid -> MfaErrorText(
            R.string.embedded_login_mfa_error_token_invalid_title,
            R.string.embedded_login_mfa_error_token_invalid,
        )
        is MfaError.TooManyAttempts -> MfaErrorText(
            R.string.embedded_login_mfa_error_too_many_attempts_title,
            R.string.embedded_login_mfa_error_too_many_attempts,
        )
        is MfaError.NoEligibleFactors -> MfaErrorText(
            R.string.embedded_login_mfa_error_no_eligible_factors_title,
            R.string.embedded_login_mfa_error_no_eligible_factors,
        )
        is MfaError.Network -> MfaErrorText(
            R.string.embedded_login_mfa_error_network_title,
            R.string.embedded_login_mfa_error_network,
        )
        is MfaError.Unexpected -> MfaErrorText(
            R.string.embedded_login_mfa_error_unexpected_title,
            R.string.embedded_login_mfa_error_unexpected,
        )
    }
