package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginCodeScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginEmailScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginNoMethodsScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginPhoneScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaConfiguration
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaHost
import com.auth0.universalcomponents.presentation.ui.flow.FlowErrorScreen
import com.auth0.universalcomponents.presentation.ui.flow.FlowLoadingScreen
import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country

/** What a screen needs from the host, without knowing about the ViewModel. */
internal class RenderContext(
    val dispatch: (LoginIntent) -> Unit,
    val back: () -> Unit,
    val mfa: MfaConfiguration = MfaConfiguration(),
)

/** The screen for a step, with the data it shows. */
internal sealed interface ScreenPlan {

    data object Loading : ScreenPlan

    /** The email or phone entry, as [method] says; [otherMethod] is the one "Continue with" switches to, if any. */
    data class Identifier(
        val value: String,
        val method: LoginOption = LoginOption.EmailOtp,
        val otherMethod: LoginOption? = null,
        val isSubmitting: Boolean = false,
        val country: Country? = null,
    ) : ScreenPlan

    data class OtpCode(
        val identifier: String,
        val method: LoginOption = LoginOption.EmailOtp,
        val error: EmbeddedLoginError? = null,
        val isSubmitting: Boolean = false,
        val isResending: Boolean = false,
        val resendSecondsLeft: Int = 0,
    ) : ScreenPlan

    /**
     * A second factor; MFA draws its own screens from [engine]. When MFA ended and the app decides what happens next
     * ([isAwaitingApp]), a spinner takes their place. It is the same screen to login, so that wait never slides MFA out
     * and back, and the error MFA ended on never shows while it does.
     */
    data class Mfa(val engine: MfaEngine, val isAwaitingApp: Boolean = false) : ScreenPlan

    data object Authenticated : ScreenPlan

    data class Failed(val error: EmbeddedLoginError) : ScreenPlan

    data object NoMethods : ScreenPlan
}

/** Names the screen and ignores its data, so a data-only change doesn't animate. */
internal val ScreenPlan.transitionIdentity: String
    get() = when (this) {
        ScreenPlan.Loading -> "loading"
        // One per method: switching between them slides, and neither screen keeps the other's typing.
        is ScreenPlan.Identifier -> "identifier:$method"
        is ScreenPlan.OtpCode -> "otpCode"
        is ScreenPlan.Mfa -> "mfa"
        ScreenPlan.Authenticated -> "authenticated"
        is ScreenPlan.Failed -> "failed"
        ScreenPlan.NoMethods -> "noMethods"
    }

/** Compared across a change: deeper pushes, shallower pops. */
internal val ScreenPlan.navigationDepth: Int
    get() = when (this) {
        ScreenPlan.Loading -> 0
        ScreenPlan.NoMethods -> 1
        // The secondary method is reached from the primary one, so it sits one deeper.
        is ScreenPlan.Identifier -> if (method == LoginOption.EmailOtp) 1 else 2
        is ScreenPlan.OtpCode -> 3
        is ScreenPlan.Mfa -> 4
        ScreenPlan.Authenticated, is ScreenPlan.Failed -> 5
    }

internal fun plan(step: LoginState): ScreenPlan = when (step) {
    LoginState.Loading -> ScreenPlan.Loading
    is LoginState.CollectOtpIdentifier ->
        ScreenPlan.Identifier(step.value, step.method, step.otherMethod, step.isSubmitting, step.country)
    is LoginState.CollectOtpCode -> ScreenPlan.OtpCode(
        identifier = step.identifier,
        method = step.method,
        error = step.error,
        isSubmitting = step.isSubmitting,
        isResending = step.isResending,
        resendSecondsLeft = step.resendSecondsLeft,
    )
    LoginState.Authenticated -> ScreenPlan.Authenticated
    is LoginState.Failed -> ScreenPlan.Failed(step.error)
    LoginState.NoMethodsAvailable -> ScreenPlan.NoMethods
    is LoginState.Mfa -> ScreenPlan.Mfa(step.engine)
    is LoginState.AwaitingApp -> ScreenPlan.Mfa(step.engine, isAwaitingApp = true)
}

/** Builds the screen for [plan]; input goes back through [context]. */
@Composable
internal fun LoginRenderer(plan: ScreenPlan, context: RenderContext) {
    when (plan) {
        ScreenPlan.Loading -> FlowLoadingScreen()

        is ScreenPlan.Identifier -> when (plan.method) {
            LoginOption.PhoneOtp -> LoginPhoneScreen(
                phone = plan.value,
                country = plan.country,
                isLoading = plan.isSubmitting,
                onContinue = { number, country ->
                    context.dispatch(LoginIntent.SubmitIdentifier(plan.method, number, country))
                },
            )

            else -> LoginEmailScreen(
                email = plan.value,
                isLoading = plan.isSubmitting,
                onContinue = { context.dispatch(LoginIntent.SubmitIdentifier(plan.method, it)) },
                onContinueWithPhone = plan.otherMethod?.let { other ->
                    {
                        context.dispatch(LoginIntent.SwitchMethod(other))
                    }
                },
            )
        }

        is ScreenPlan.OtpCode -> LoginCodeScreen(
            subtitle = stringResource(R.string.embedded_login_code_from, plan.identifier),
            isLoading = plan.isSubmitting,
            isResending = plan.isResending,
            resendSecondsLeft = plan.resendSecondsLeft,
            errorMessage = plan.error?.text(plan.method),
            onVerify = { context.dispatch(LoginIntent.SubmitOtp(it)) },
            onEdit = context.back,
            onResend = { context.dispatch(LoginIntent.Resend) },
        )

        // MfaHost knows what a headless app leaves to login: the wait for the app, and the "not available" screen.
        is ScreenPlan.Mfa -> MfaHost(
            plan.engine,
            onLeave = { context.dispatch(LoginIntent.LeaveMfa) },
            configuration = context.mfa,
            isAwaitingApp = plan.isAwaitingApp,
        )

        ScreenPlan.Authenticated -> LoginSuccessScreen()

        is ScreenPlan.Failed -> FlowErrorScreen(
            message = plan.error.text(),
            onRetry = { context.dispatch(LoginIntent.Retry) },
        )

        ScreenPlan.NoMethods -> LoginNoMethodsScreen(onBack = context.back)
    }
}

/**
 * Our words for each error; the server's own when it explained one we don't know. [method] is where the code went,
 * for the words that name it.
 */
@Composable
internal fun EmbeddedLoginError.text(method: LoginOption = LoginOption.EmailOtp): String = when (this) {
    EmbeddedLoginError.ConnectionMissing -> stringResource(R.string.embedded_login_error_connection_missing)
    EmbeddedLoginError.Network -> stringResource(R.string.embedded_login_error_network)
    is EmbeddedLoginError.CodeNotSent -> stringResource(
        if (this.method == LoginOption.PhoneOtp) {
            R.string.embedded_login_error_code_not_sent_phone
        } else {
            R.string.embedded_login_error_code_not_sent
        },
    )
    EmbeddedLoginError.InvalidCode -> stringResource(
        if (method == LoginOption.PhoneOtp) {
            R.string.embedded_login_error_invalid_code_phone
        } else {
            R.string.embedded_login_error_invalid_code
        },
    )
    EmbeddedLoginError.CodeExpired -> stringResource(R.string.embedded_login_error_code_expired)
    EmbeddedLoginError.TooManyWrongCodes -> stringResource(R.string.embedded_login_error_too_many_wrong_codes)
    EmbeddedLoginError.TooManyAttempts -> stringResource(R.string.embedded_login_error_too_many_attempts)
    EmbeddedLoginError.TooManyLogins -> stringResource(R.string.embedded_login_error_too_many_logins)
    EmbeddedLoginError.SessionExpired -> stringResource(R.string.embedded_login_error_session_expired)
    is EmbeddedLoginError.InvalidIdentifier -> stringResource(
        if (this.method == LoginOption.PhoneOtp) {
            R.string.embedded_login_error_invalid_phone_format
        } else {
            R.string.embedded_login_error_invalid_email_format
        },
    )
    is EmbeddedLoginError.Server -> text
    // Handed over to MFA before any screen shows it; the generic text is only a safe fallback.
    is EmbeddedLoginError.MfaRequired,
    is EmbeddedLoginError.Unknown -> stringResource(R.string.embedded_login_error_generic)
}
