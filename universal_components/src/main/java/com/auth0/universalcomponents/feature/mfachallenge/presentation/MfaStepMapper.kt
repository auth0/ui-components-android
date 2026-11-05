package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListHeader
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListModel
import com.auth0.universalcomponents.feature.mfachallenge.ErrorModel
import com.auth0.universalcomponents.feature.mfachallenge.LoadingModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaExit
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorId
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorStep
import com.auth0.universalcomponents.feature.mfachallenge.MfaStep
import com.auth0.universalcomponents.feature.mfachallenge.MfaStepStream
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor

/**
 * What this state shows, as the public [MfaStep]: the one place the models are built, so the standard screens, a screen
 * the app replaced and a headless app all get the same data. Every action goes back as an [MfaIntent] through
 * [onIntent]; [onLeave] is the error screen's way out, which belongs to the host because the engine has stopped by
 * then. Pure, so a test needs no screen.
 */
internal fun MfaState.toMfaStep(onIntent: (MfaIntent) -> Unit, onLeave: () -> Unit): MfaStep {
    val cancel = { onIntent(MfaIntent.Cancel) }
    return when (val state = this) {
        is MfaState.ChooseFactor -> MfaStep.AuthenticatorList(listModel(state.round, pending = null, cancel, onIntent))

        // The challenge goes out before its screen: over the list its row is busy, otherwise a short loading shows.
        is MfaState.SendingChallenge ->
            if (state.round.showsList) {
                MfaStep.AuthenticatorList(listModel(state.round, pending = state.factor, cancel, onIntent))
            } else {
                MfaStep.Loading(LoadingModel(cancel))
            }

        is MfaState.PushWaiting -> MfaStep.Factor(
            MfaFactorStep(
                factor = MfaFactorId.Push,
                round = state.round.number,
                model = PushChallengeModel(
                    deviceName = state.factor.deviceName,
                    canResend = state.canResend,
                    resendAvailableIn = state.resendSecondsLeft,
                    canSwitch = state.round.canSwitch,
                    isResending = state.phase == PushPhase.Resending,
                    resend = { onIntent(MfaIntent.Resend) },
                    switchFactor = { onIntent(MfaIntent.ChooseAnotherFactor) },
                    cancel = cancel,
                ),
            ),
        )

        is MfaState.Failed -> MfaStep.Error(ErrorModel(state.error, MfaExit.StartOver, leave = onLeave))
    }
}

private fun listModel(
    round: MfaRound,
    pending: MfaFactor?,
    cancel: () -> Unit,
    onIntent: (MfaIntent) -> Unit,
) = AuthenticatorListModel(
    header = if (round.isAdditional) AuthenticatorListHeader.ChallengeAdditional else AuthenticatorListHeader.Challenge,
    round = round.number,
    rows = round.factors.map { it.row() },
    pendingRowId = pending?.rowId,
    select = { rowId ->
        round.factors.firstOrNull { it.rowId == rowId }?.let { onIntent(MfaIntent.SelectFactor(it)) }
    },
    cancel = cancel,
)

private fun MfaFactor.row() = AuthenticatorListModel.Row(
    id = rowId,
    factor = factorId,
    title = when (this) {
        is MfaFactor.Push -> deviceName
    },
    subtitle = null,
    icon = when (this) {
        is MfaFactor.Push -> R.drawable.ic_device
    },
)

private val MfaFactor.factorId: MfaFactorId
    get() = when (this) {
        is MfaFactor.Push -> MfaFactorId.Push
    }

/** One row per device: the server's handle for the device tells two phones with push apart. */
private val MfaFactor.rowId: String
    get() = when (this) {
        is MfaFactor.Push -> "${MfaFactorId.Push.value}:$index"
    }

/**
 * Keeps a headless app's [MfaStepStream] showing what [engine] shows, as the same models the standard screens draw.
 * Runs until its coroutine is cancelled; the host then clears the stream. Taps the app makes on a model come back to
 * [engine] as intents; the error screen's way out is [onLeave].
 *
 * When no factor can be used there is no step to publish, whatever the app answered: the app is told through
 * `onMfaError` and the result, and the login draws the "not available" screen if the app didn't take over, as on iOS.
 */
internal suspend fun MfaStepStream.mirror(engine: MfaEngine, onLeave: () -> Unit): Nothing {
    engine.state.collect { state ->
        val step = if (state.isNoEligibleFactors) null else state.toMfaStep(engine::onIntent, onLeave)
        showStep(step)
    }
}

/** MFA ended because none of the factors on offer can be used here. */
internal val MfaState.isNoEligibleFactors: Boolean
    get() = this is MfaState.Failed && error is MfaError.NoEligibleFactors
