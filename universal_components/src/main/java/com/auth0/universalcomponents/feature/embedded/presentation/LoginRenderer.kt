package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.AuthFailure
import com.auth0.universalcomponents.feature.embedded.domain.model.FailureMessage
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginEvent
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginStep
import com.auth0.universalcomponents.feature.embedded.domain.model.MfaFactor
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginCodeScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginEmailScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginLoadingScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginNoMethodsScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen

/** What a screen needs from the host, without knowing about the ViewModel. */
internal class RenderContext(
    val isSubmitting: Boolean,
    val isResending: Boolean,
    val inputError: AuthFailure?,
    val dispatch: (LoginEvent) -> Unit,
    val resend: () -> Unit,
    val back: () -> Unit,
)

/** The screen for a step, with the data it shows. */
internal sealed interface ScreenPlan {

    data object Loading : ScreenPlan

    data class Identifier(val email: String) : ScreenPlan

    data class OtpCode(val identifier: String) : ScreenPlan

    data class MfaCode(val factor: MfaFactor, val destination: String?) : ScreenPlan

    data object Authenticated : ScreenPlan

    data class Failed(val message: FailureMessage) : ScreenPlan

    data object NoMethods : ScreenPlan
}

/** Names the screen and ignores its data, so a data-only change doesn't animate. */
internal val ScreenPlan.transitionIdentity: String
    get() = when (this) {
        ScreenPlan.Loading -> "loading"
        is ScreenPlan.Identifier -> "identifier"
        is ScreenPlan.OtpCode -> "otpCode"
        is ScreenPlan.MfaCode -> "mfaCode.$factor"
        ScreenPlan.Authenticated -> "authenticated"
        is ScreenPlan.Failed -> "failed"
        ScreenPlan.NoMethods -> "noMethods"
    }

/** Compared across a change: deeper pushes, shallower pops. */
internal val ScreenPlan.navigationDepth: Int
    get() = when (this) {
        ScreenPlan.Loading -> 0
        is ScreenPlan.Identifier, ScreenPlan.NoMethods -> 1
        is ScreenPlan.OtpCode, is ScreenPlan.MfaCode -> 2
        ScreenPlan.Authenticated, is ScreenPlan.Failed -> 3
    }

internal fun plan(step: LoginStep): ScreenPlan = when (step) {
    LoginStep.Loading -> ScreenPlan.Loading
    is LoginStep.CollectOtpIdentifier -> ScreenPlan.Identifier(step.email)
    is LoginStep.CollectOtpCode -> ScreenPlan.OtpCode(step.identifier)
    is LoginStep.StepUpMfa -> ScreenPlan.MfaCode(step.challenge.factor, step.challenge.destination)
    LoginStep.Authenticated -> ScreenPlan.Authenticated
    is LoginStep.Failed -> ScreenPlan.Failed(step.message)
    LoginStep.NoMethodsAvailable -> ScreenPlan.NoMethods
}

/** Builds the screen for [plan]; input goes back through [context]. */
@Composable
internal fun LoginRenderer(plan: ScreenPlan, context: RenderContext) {
    val errorMessage = context.inputError?.let { stringResource(it.messageRes) }
    when (plan) {
        ScreenPlan.Loading -> LoginLoadingScreen()

        is ScreenPlan.Identifier -> LoginEmailScreen(
            email = plan.email,
            isLoading = context.isSubmitting,
            errorMessage = errorMessage,
            onContinue = { context.dispatch(LoginEvent.SubmitIdentifier(it)) },
        )

        is ScreenPlan.OtpCode -> LoginCodeScreen(
            subtitle = stringResource(R.string.embedded_login_code_from, plan.identifier),
            isLoading = context.isSubmitting,
            isResending = context.isResending,
            // No usable code is on its way, so waiting out the cooldown would gain nothing.
            canResendNow = context.inputError == AuthFailure.CODE_EXPIRED || context.inputError == AuthFailure.CODE_NOT_SENT,
            errorMessage = errorMessage,
            onVerify = { context.dispatch(LoginEvent.SubmitOtp(it)) },
            onEdit = context.back,
            onResend = context.resend,
        )

        is ScreenPlan.MfaCode -> LoginCodeScreen(
            subtitle = plan.factor.codeSourceHint(plan.destination),
            isLoading = context.isSubmitting,
            errorMessage = errorMessage,
            onVerify = { context.dispatch(LoginEvent.SubmitMfa(it)) },
        )

        ScreenPlan.Authenticated -> LoginSuccessScreen()

        is ScreenPlan.Failed -> LoginErrorScreen(
            message = plan.message.text(),
            onRetry = { context.dispatch(LoginEvent.Retry) },
        )

        ScreenPlan.NoMethods -> LoginNoMethodsScreen(onBack = context.back)
    }
}

/** Subtitle for the step-up code screen. */
@Composable
private fun MfaFactor.codeSourceHint(destination: String?): String = when {
    !destination.isNullOrEmpty() -> stringResource(R.string.embedded_login_code_from, destination)
    this == MfaFactor.OTP -> stringResource(R.string.embedded_login_code_from_authenticator)
    else -> stringResource(R.string.embedded_login_code_from_email)
}

@Composable
private fun FailureMessage.text(): String = when (this) {
    is FailureMessage.Server -> text
    is FailureMessage.Local -> stringResource(textRes)
}
