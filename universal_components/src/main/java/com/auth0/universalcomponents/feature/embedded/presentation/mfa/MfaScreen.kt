package com.auth0.universalcomponents.feature.embedded.presentation.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaFactorListScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.MfaPushScreen
import com.auth0.universalcomponents.feature.embedded.presentation.text

/**
 * Draws one [MfaStep] and sends every tap back as an [MfaIntent]. It holds no logic, so whoever hosts MFA draws it
 * inside its own frame: embedded login does, so moving into MFA animates like any other step.
 */
@Composable
internal fun MfaScreen(step: MfaStep, onIntent: (MfaIntent) -> Unit) {
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
internal val MfaStep.transitionIdentity: String
    get() = when (this) {
        is MfaStep.AuthenticatorList -> "mfaList"
        is MfaStep.PushWaiting -> "mfaPush"
        is MfaStep.Failed -> "mfaFailed"
    }

/** The login wording, except where it assumes an emailed first-factor code. */
@Composable
private fun EmbeddedLoginError.mfaText(): String = when (this) {
    EmbeddedLoginError.CodeExpired -> stringResource(R.string.embedded_login_mfa_error_expired)
    else -> text()
}
