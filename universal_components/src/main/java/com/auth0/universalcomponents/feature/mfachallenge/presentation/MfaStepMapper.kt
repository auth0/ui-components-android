package com.auth0.universalcomponents.feature.mfachallenge.presentation

import com.auth0.android.embedded.authorize.PhoneDeliveryMethod
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListHeader
import com.auth0.universalcomponents.feature.mfachallenge.AuthenticatorListModel
import com.auth0.universalcomponents.feature.mfachallenge.EmailChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.ErrorModel
import com.auth0.universalcomponents.feature.mfachallenge.LoadingModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaExit
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorId
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorModel
import com.auth0.universalcomponents.feature.mfachallenge.MfaFactorStep
import com.auth0.universalcomponents.feature.mfachallenge.MfaStep
import com.auth0.universalcomponents.feature.mfachallenge.MfaStepStream
import com.auth0.universalcomponents.feature.mfachallenge.PhoneChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.PhoneDelivery
import com.auth0.universalcomponents.feature.mfachallenge.PushChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.RecoveryCodeChallengeModel
import com.auth0.universalcomponents.feature.mfachallenge.TotpChallengeModel
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

        is MfaState.CodeEntry -> MfaStep.Factor(
            MfaFactorStep(
                factor = state.factor.factorId,
                round = state.round.number,
                model = state.toModel(onIntent, cancel),
            ),
        )

        // The new code on show: nothing to switch to, so that tap does nothing; Cancel leaves, and the code is lost.
        is MfaState.SaveRecoveryCode -> MfaStep.Factor(
            MfaFactorStep(
                factor = MfaFactorId.RecoveryCode,
                round = state.round.number,
                model = RecoveryCodeChallengeModel(
                    newCode = state.newCode,
                    isVerifying = state.isConfirming,
                    requestError = state.requestError,
                    confirm = { onIntent(MfaIntent.ConfirmRecoveryCode) },
                    cancel = cancel,
                ),
            ),
        )

        is MfaState.Failed -> MfaStep.Error(ErrorModel(state.error, MfaExit.StartOver, leave = onLeave))
    }
}

/**
 * The code screen's model: the same actions for a code sent to a phone or to an email address, and for the one an
 * authenticator app shows or a recovery code, which have no Resend.
 */
private fun MfaState.CodeEntry.toModel(onIntent: (MfaIntent) -> Unit, cancel: () -> Unit): MfaFactorModel {
    val submit = { code: String -> onIntent(MfaIntent.SubmitCode(code)) }
    val resendTapped = { onIntent(MfaIntent.Resend) }
    val switchFactor = { onIntent(MfaIntent.ChooseAnotherFactor) }
    return when (val factor = factor) {
        is MfaFactor.Phone -> PhoneChallengeModel(
            phoneNumber = factor.maskedNumber,
            delivery = factor.delivery.toDelivery(),
            isWrongCode = isWrongCode,
            isVerifying = phase == CodePhase.Verifying,
            isResending = phase == CodePhase.Resending,
            canResend = resend?.isOffered == true,
            resendAvailableIn = resend?.secondsLeft ?: 0,
            canSwitch = round.canSwitch,
            submit = submit,
            resend = resendTapped,
            switchFactor = switchFactor,
            cancel = cancel,
            requestError = requestError,
            isCodeExpired = isCodeExpired,
        )

        is MfaFactor.Email -> EmailChallengeModel(
            address = factor.address,
            isWrongCode = isWrongCode,
            isVerifying = phase == CodePhase.Verifying,
            isResending = phase == CodePhase.Resending,
            canResend = resend?.isOffered == true,
            resendAvailableIn = resend?.secondsLeft ?: 0,
            canSwitch = round.canSwitch,
            submit = submit,
            resend = resendTapped,
            switchFactor = switchFactor,
            cancel = cancel,
            requestError = requestError,
            isCodeExpired = isCodeExpired,
        )

        MfaFactor.AuthenticatorApp -> TotpChallengeModel(
            isWrongCode = isWrongCode,
            isVerifying = phase == CodePhase.Verifying,
            canSwitch = round.canSwitch,
            submit = submit,
            switchFactor = switchFactor,
            cancel = cancel,
            requestError = requestError,
        )

        MfaFactor.RecoveryCode -> RecoveryCodeChallengeModel(
            isWrongCode = isWrongCode,
            isVerifying = phase == CodePhase.Verifying,
            requestError = requestError,
            canSwitch = round.canSwitch,
            submit = submit,
            switchFactor = switchFactor,
            cancel = cancel,
        )
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
        is MfaFactor.Phone -> maskedNumber
        is MfaFactor.Email -> address
        // The user's own app and codes have no name to show: the row says what the factor is.
        MfaFactor.AuthenticatorApp, MfaFactor.RecoveryCode -> ""
    },
    subtitle = null,
    icon = when (this) {
        is MfaFactor.Push -> R.drawable.ic_device
        is MfaFactor.Phone ->
            if (delivery == PhoneDeliveryMethod.VOICE) R.drawable.ic_mfa_voice else R.drawable.ic_mfa_sms
        is MfaFactor.Email -> R.drawable.ic_mfa_email
        MfaFactor.AuthenticatorApp -> R.drawable.ic_authenticator
        MfaFactor.RecoveryCode -> R.drawable.ic_recovery_code
    },
    delivery = (this as? MfaFactor.Phone)?.delivery?.toDelivery(),
)

private val MfaFactor.factorId: MfaFactorId
    get() = when (this) {
        is MfaFactor.Push -> MfaFactorId.Push
        is MfaFactor.Phone -> MfaFactorId.Phone
        is MfaFactor.Email -> MfaFactorId.Email
        MfaFactor.AuthenticatorApp -> MfaFactorId.Totp
        MfaFactor.RecoveryCode -> MfaFactorId.RecoveryCode
    }

/**
 * One row per device, per way a phone is reached, and per email address: the server's handle for the device tells two
 * phones with push apart, and a phone that can be texted and called is two rows. The authenticator app is one row.
 */
private val MfaFactor.rowId: String
    get() = when (this) {
        is MfaFactor.Push -> "${MfaFactorId.Push.value}:$index"
        is MfaFactor.Phone -> "${MfaFactorId.Phone.value}:$index:${delivery.name}"
        is MfaFactor.Email -> "${MfaFactorId.Email.value}:$index"
        MfaFactor.AuthenticatorApp -> MfaFactorId.Totp.value
        MfaFactor.RecoveryCode -> MfaFactorId.RecoveryCode.value
    }

private fun PhoneDeliveryMethod.toDelivery() = when (this) {
    PhoneDeliveryMethod.TEXT -> PhoneDelivery.Text
    PhoneDeliveryMethod.VOICE -> PhoneDelivery.Voice
}

/**
 * Keeps a headless app's [MfaStepStream] showing what [engine] shows, as the same models the standard screens draw.
 * Runs until its coroutine is cancelled; the host then clears the stream. Taps the app makes on a model come back to
 * [engine] as intents; the error screen's way out is [onLeave].
 *
 * When no factor can be used there is no step to publish, whatever the app answered: the app is told through
 * `onMfaError` and the result, and the login draws the "not available" screen if the app didn't take over.
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
