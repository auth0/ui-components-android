package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginCodeScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginEmailScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginLoadingScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginNoMethodsScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen

/** What a screen needs from the host, without knowing about the ViewModel. */
internal class RenderContext(
    val dispatch: (LoginEvent) -> Unit,
    val back: () -> Unit,
)

/** The screen for a step, with the data it shows. */
internal sealed interface ScreenPlan {

    data object Loading : ScreenPlan

    data class Identifier(val email: String, val isSubmitting: Boolean = false) : ScreenPlan

    data class OtpCode(
        val identifier: String,
        val error: EmbeddedLoginError? = null,
        val isSubmitting: Boolean = false,
        val isResending: Boolean = false,
        val resendSecondsLeft: Int = 0,
    ) : ScreenPlan

    data object Authenticated : ScreenPlan

    data class Failed(val error: EmbeddedLoginError) : ScreenPlan

    data object NoMethods : ScreenPlan
}

/** Names the screen and ignores its data, so a data-only change doesn't animate. */
internal val ScreenPlan.transitionIdentity: String
    get() = when (this) {
        ScreenPlan.Loading -> "loading"
        is ScreenPlan.Identifier -> "identifier"
        is ScreenPlan.OtpCode -> "otpCode"
        ScreenPlan.Authenticated -> "authenticated"
        is ScreenPlan.Failed -> "failed"
        ScreenPlan.NoMethods -> "noMethods"
    }

/** Compared across a change: deeper pushes, shallower pops. */
internal val ScreenPlan.navigationDepth: Int
    get() = when (this) {
        ScreenPlan.Loading -> 0
        is ScreenPlan.Identifier, ScreenPlan.NoMethods -> 1
        is ScreenPlan.OtpCode -> 2
        ScreenPlan.Authenticated, is ScreenPlan.Failed -> 3
    }

internal fun plan(step: LoginStep): ScreenPlan = when (step) {
    LoginStep.Loading -> ScreenPlan.Loading
    is LoginStep.CollectOtpIdentifier -> ScreenPlan.Identifier(step.email, step.isSubmitting)
    is LoginStep.CollectOtpCode -> ScreenPlan.OtpCode(
        identifier = step.identifier,
        error = step.error,
        isSubmitting = step.isSubmitting,
        isResending = step.isResending,
        resendSecondsLeft = step.resendSecondsLeft,
    )
    LoginStep.Authenticated -> ScreenPlan.Authenticated
    is LoginStep.Failed -> ScreenPlan.Failed(step.error)
    LoginStep.NoMethodsAvailable -> ScreenPlan.NoMethods
}

/** Builds the screen for [plan]; input goes back through [context]. */
@Composable
internal fun LoginRenderer(plan: ScreenPlan, context: RenderContext) {
    when (plan) {
        ScreenPlan.Loading -> LoginLoadingScreen()

        is ScreenPlan.Identifier -> LoginEmailScreen(
            email = plan.email,
            isLoading = plan.isSubmitting,
            onContinue = { context.dispatch(LoginEvent.SubmitIdentifier(it)) },
        )

        is ScreenPlan.OtpCode -> LoginCodeScreen(
            subtitle = stringResource(R.string.embedded_login_code_from, plan.identifier),
            isLoading = plan.isSubmitting,
            isResending = plan.isResending,
            resendSecondsLeft = plan.resendSecondsLeft,
            errorMessage = plan.error?.text(),
            onVerify = { context.dispatch(LoginEvent.SubmitOtp(it)) },
            onEdit = context.back,
            onResend = { context.dispatch(LoginEvent.Resend) },
        )

        ScreenPlan.Authenticated -> LoginSuccessScreen()

        is ScreenPlan.Failed -> LoginErrorScreen(
            message = plan.error.text(),
            onRetry = { context.dispatch(LoginEvent.Retry) },
        )

        ScreenPlan.NoMethods -> LoginNoMethodsScreen(onBack = context.back)
    }
}

/** Our words for each error; the server's own when it explained one we don't know. */
@Composable
private fun EmbeddedLoginError.text(): String = when (this) {
    EmbeddedLoginError.ConnectionMissing -> stringResource(R.string.embedded_login_error_connection_missing)
    EmbeddedLoginError.Network -> stringResource(R.string.embedded_login_error_network)
    EmbeddedLoginError.CodeNotSent -> stringResource(R.string.embedded_login_error_code_not_sent)
    EmbeddedLoginError.InvalidCode -> stringResource(R.string.embedded_login_error_invalid_code)
    EmbeddedLoginError.CodeExpired -> stringResource(R.string.embedded_login_error_code_expired)
    EmbeddedLoginError.TooManyWrongCodes -> stringResource(R.string.embedded_login_error_too_many_wrong_codes)
    EmbeddedLoginError.TooManyAttempts -> stringResource(R.string.embedded_login_error_too_many_attempts)
    EmbeddedLoginError.TooManyLogins -> stringResource(R.string.embedded_login_error_too_many_logins)
    EmbeddedLoginError.ExtraStepUnsupported -> stringResource(R.string.embedded_login_error_extra_step_unsupported)
    is EmbeddedLoginError.Server -> text
    is EmbeddedLoginError.Unknown -> stringResource(R.string.embedded_login_error_generic)
}
