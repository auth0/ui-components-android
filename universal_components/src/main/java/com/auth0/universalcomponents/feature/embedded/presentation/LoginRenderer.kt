package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginCodeScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginEmailScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginErrorScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginLoadingScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginNoMethodsScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaConfiguration
import com.auth0.universalcomponents.feature.mfachallenge.MfaPresentation
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaHost
import com.auth0.universalcomponents.feature.mfachallenge.presentation.isNoEligibleFactors

/** What a screen needs from the host, without knowing about the ViewModel. */
internal class RenderContext(
    val dispatch: (LoginEvent) -> Unit,
    val back: () -> Unit,
    val mfa: MfaConfiguration = MfaConfiguration(),
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

    /** A second factor; MFA draws its own screens from [engine]. */
    data class Mfa(val engine: MfaEngine) : ScreenPlan

    /** MFA ended and the app decides what happens next; a spinner shows until it does. */
    data object AwaitingApp : ScreenPlan

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
        is ScreenPlan.Mfa -> "mfa"
        ScreenPlan.AwaitingApp -> "awaitingApp"
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
        is ScreenPlan.Mfa, ScreenPlan.AwaitingApp -> 3
        ScreenPlan.Authenticated, is ScreenPlan.Failed -> 5
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
    is LoginStep.Mfa -> ScreenPlan.Mfa(step.engine)
    is LoginStep.AwaitingApp -> ScreenPlan.AwaitingApp
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

        // A headless app draws MFA itself, from its step stream, except the "not available" screen: that one is login's.
        is ScreenPlan.Mfa -> {
            val state by plan.engine.step.collectAsStateWithLifecycle()
            if (context.mfa.presentation == MfaPresentation.Standard || state.isNoEligibleFactors) {
                MfaHost(plan.engine, onLeave = { context.dispatch(LoginEvent.LeaveMfa) }, configuration = context.mfa)
            }
        }

        ScreenPlan.AwaitingApp -> LoginLoadingScreen(showAtOnce = true)

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
internal fun EmbeddedLoginError.text(): String = when (this) {
    EmbeddedLoginError.ConnectionMissing -> stringResource(R.string.embedded_login_error_connection_missing)
    EmbeddedLoginError.Network -> stringResource(R.string.embedded_login_error_network)
    EmbeddedLoginError.CodeNotSent -> stringResource(R.string.embedded_login_error_code_not_sent)
    EmbeddedLoginError.InvalidCode -> stringResource(R.string.embedded_login_error_invalid_code)
    EmbeddedLoginError.CodeExpired -> stringResource(R.string.embedded_login_error_code_expired)
    EmbeddedLoginError.TooManyWrongCodes -> stringResource(R.string.embedded_login_error_too_many_wrong_codes)
    EmbeddedLoginError.TooManyAttempts -> stringResource(R.string.embedded_login_error_too_many_attempts)
    EmbeddedLoginError.TooManyLogins -> stringResource(R.string.embedded_login_error_too_many_logins)
    EmbeddedLoginError.SessionExpired -> stringResource(R.string.embedded_login_error_session_expired)
    is EmbeddedLoginError.Server -> text
    // Handed over to MFA before any screen shows it; the generic text is only a safe fallback.
    is EmbeddedLoginError.MfaRequired,
    is EmbeddedLoginError.Unknown -> stringResource(R.string.embedded_login_error_generic)
}
