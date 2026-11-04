package com.auth0.universalcomponents.feature.mfachallenge.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.universalcomponents.feature.mfachallenge.LoadingModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaAuthenticatorListScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaConfiguration
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaLoadingScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaPushChallengeScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaStep
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.ResolvedMfaMessages
import com.auth0.universalcomponents.feature.mfachallenge.resolve
import com.auth0.universalcomponents.presentation.ui.utils.stepTransition

/**
 * Draws [engine]'s steps and sends every tap straight back to it, so whoever hosts MFA needs to know nothing of its
 * screens or taps. Each step is drawn by the app's screen when [configuration] replaces it, otherwise by the standard
 * one, from the same model. It draws no frame of its own: the host places it inside its frame (embedded login's), so
 * moving into MFA animates like any other step, and moving between MFA screens animates here. A headless app draws
 * everything itself, so the host doesn't place this at all.
 *
 * Once MFA has ended, its error screen is all that's left and the engine takes no more taps, so "Start over" calls
 * [onLeave]: the host decides where it goes (embedded login: back to email entry).
 *
 * While [isAwaitingApp], MFA has ended and the app decides what happens next: a spinner (with the loading label for
 * screen readers) takes the place of the step, so the error MFA ended on never shows if the app takes over.
 */
@Composable
internal fun MfaHost(
    engine: MfaEngine,
    onLeave: () -> Unit,
    configuration: MfaConfiguration,
    modifier: Modifier = Modifier,
    isAwaitingApp: Boolean = false,
) {
    val state by engine.step.collectAsStateWithLifecycle()
    val messages = configuration.messages.resolve()
    AnimatedContent(
        targetState = if (isAwaitingApp) null else state,
        contentKey = { it.transitionIdentity },
        transitionSpec = {
            stepTransition(isForward = targetState.navigationDepth >= initialState.navigationDepth)
        },
        modifier = modifier.fillMaxSize(),
        label = "mfaStep",
    ) { shown ->
        if (shown == null) {
            MfaLoadingScreen(LoadingModel(), messages = messages)
        } else {
            MfaScreen(project(shown, onIntent = engine::onIntent, onLeave = onLeave), configuration, messages)
        }
    }
}

/** Draws one [MfaStep], with the app's screen where it replaced the standard one. It holds no logic. */
@Composable
private fun MfaScreen(step: MfaStep, configuration: MfaConfiguration, messages: ResolvedMfaMessages) {
    val screens = configuration.screens
    when (step) {
        is MfaStep.Loading -> MfaLoadingScreen(step.model, messages = messages)

        is MfaStep.AuthenticatorList -> {
            val replacement = screens.authenticatorList
            if (replacement != null) {
                replacement(step.model, messages)
            } else {
                MfaAuthenticatorListScreen(step.model, messages = messages)
            }
        }

        is MfaStep.Factor -> {
            val replacement = screens.factors[step.step.factor]
            if (replacement != null) {
                replacement(step.step.model, messages)
            } else {
                when (val model = step.step.model) {
                    is PushChallengeModel -> MfaPushChallengeScreen(model, messages = messages)
                }
            }
        }

        is MfaStep.Error -> {
            val replacement = screens.error
            if (replacement != null) {
                replacement(step.model, messages)
            } else {
                MfaErrorScreen(step.model, messages = messages)
            }
        }
    }
}

/**
 * Names the screen and ignores its data, so a data-only change doesn't animate. A challenge going out keeps the list
 * it was picked from, and each round's list is a new screen. Null is the wait for the app.
 */
private val MfaState?.transitionIdentity: String
    get() = when (this) {
        null -> "awaitingApp"
        is MfaState.ChooseFactor -> "list:${round.number}"
        is MfaState.SendingChallenge -> if (round.showsList) "list:${round.number}" else "sending:${round.number}"
        is MfaState.PushWaiting -> "push:${round.number}"
        is MfaState.Failed -> "failed"
    }

/**
 * Compared across a change: deeper pushes, shallower pops. Each round goes deeper than the one before, and the wait
 * for the app comes after MFA's last screen.
 */
private val MfaState?.navigationDepth: Int
    get() = when (this) {
        null -> Int.MAX_VALUE
        is MfaState.ChooseFactor -> round.depth
        is MfaState.SendingChallenge -> round.depth
        is MfaState.PushWaiting -> round.depth + 1
        is MfaState.Failed -> Int.MAX_VALUE
    }

private val MfaRound.depth: Int
    get() = number * 2
