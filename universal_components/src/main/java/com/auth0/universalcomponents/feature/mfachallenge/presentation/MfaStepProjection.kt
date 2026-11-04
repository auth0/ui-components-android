package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListHeader
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListModel
import com.auth0.universalcomponents.feature.mfachallenge.ErrorModel
import com.auth0.universalcomponents.feature.mfachallenge.LoadingModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaAction
import com.auth0.universalcomponents.feature.mfachallenge.MfaExit
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorId
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorStep
import com.auth0.universalcomponents.feature.mfachallenge.MfaSelectAction
import com.auth0.universalcomponents.feature.mfachallenge.MfaStep
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.domain.model.MfaFactor

/**
 * What [state] shows, as the public [MfaStep]: the one place the models are built, so the standard screens, a screen
 * the app replaced and a headless app all get the same data. Every action goes back as an [MfaIntent] through
 * [onIntent]; [onLeave] is the error screen's way out, which belongs to the host because the engine has stopped by
 * then. Pure, so a test needs no screen.
 */
internal fun project(state: MfaState, onIntent: (MfaIntent) -> Unit, onLeave: () -> Unit): MfaStep {
    val cancel = MfaAction { onIntent(MfaIntent.Cancel) }
    return when (state) {
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
                    resend = MfaAction { onIntent(MfaIntent.Resend) },
                    switchFactor = MfaAction { onIntent(MfaIntent.ChooseAnotherFactor) },
                    cancel = cancel,
                ),
            ),
        )

        is MfaState.Failed -> MfaStep.Error(ErrorModel(state.error, MfaExit.StartOver, MfaAction { onLeave() }))
    }
}

private fun listModel(
    round: MfaRound,
    pending: MfaFactor?,
    cancel: MfaAction,
    onIntent: (MfaIntent) -> Unit,
) = AuthenticatorListModel(
    header = if (round.isAdditional) AuthenticatorListHeader.ChallengeAdditional else AuthenticatorListHeader.Challenge,
    round = round.number,
    rows = round.factors.map { it.row() },
    pendingRowId = pending?.rowId,
    select = MfaSelectAction { rowId ->
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
