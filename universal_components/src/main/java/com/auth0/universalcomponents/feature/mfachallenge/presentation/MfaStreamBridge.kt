package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaStepStream

/**
 * Keeps a headless app's [MfaStepStream] showing what [engine] shows, as the same models the standard screens draw.
 * Runs until its coroutine is cancelled; the host then clears the stream. Taps the app makes on a model come back to
 * [engine] as intents; the error screen's way out is [onLeave].
 *
 * When no factor can be used there is no step to publish, whatever the app answered: the app is told through
 * `onMfaError` and the result, and the login draws the "not available" screen if the app didn't take over, as on iOS.
 */
internal suspend fun MfaStepStream.mirror(engine: MfaEngine, onLeave: () -> Unit): Nothing {
    engine.step.collect { state ->
        val step = if (state.isNoEligibleFactors) null else project(state, engine::onIntent, onLeave)
        showStep(step)
    }
}

/** MFA ended because none of the factors on offer can be used here. */
internal val MfaState.isNoEligibleFactors: Boolean
    get() = this is MfaState.Failed && error is MfaError.NoEligibleFactors
