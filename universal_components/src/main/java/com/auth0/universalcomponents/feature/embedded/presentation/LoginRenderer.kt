package com.auth0.universalcomponents.feature.embedded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.embedded.presentation.components.continueWithIcon
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginCodeScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginEmailScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginNoMethodsScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginPasswordScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginPhoneScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginSuccessScreen
import com.auth0.universalcomponents.feature.embedded.presentation.screens.LoginUsernameScreen
import com.auth0.universalcomponents.feature.mfachallenge.MfaConfiguration
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaHost
import com.auth0.universalcomponents.feature.mfachallenge.presentation.forDisplay
import com.auth0.universalcomponents.feature.mfachallenge.presentation.screens.NO_CONTEXTUAL_ALTERNATES
import com.auth0.universalcomponents.presentation.ui.flow.FlowErrorScreen
import com.auth0.universalcomponents.presentation.ui.flow.FlowLoadingScreen
import com.auth0.universalcomponents.presentation.ui.mfa.phone.Country

/** What a screen needs from the host, without knowing about the ViewModel. [typedPassword] outlives a rotation. */
internal class RenderContext(
    val dispatch: (LoginIntent) -> Unit,
    val back: () -> Unit,
    val mfa: MfaConfiguration = MfaConfiguration(),
    val typedPassword: MutableState<String> = mutableStateOf(""),
)

/** The screen for a step, with the data it shows. */
internal sealed interface ScreenPlan {

    data object Loading : ScreenPlan

    /**
     * The email, phone or username entry, as [method] says; [otherMethods] are the ones "Continue with" switches to, in
     * the tenant's order. [isFirst] is false on a method's own screen, opened from the first one. [error] is what the
     * server said that the user can fix here.
     */
    data class Identifier(
        val value: String,
        val method: LoginOption = LoginOption.EmailOtp,
        val otherMethods: List<LoginOption> = emptyList(),
        val isSubmitting: Boolean = false,
        val country: Country? = null,
        val error: EmbeddedLoginError? = null,
        val isFirst: Boolean = true,
    ) : ScreenPlan

    data class OtpCode(
        val identifier: String,
        val method: LoginOption = LoginOption.EmailOtp,
        val error: EmbeddedLoginError? = null,
        val isSubmitting: Boolean = false,
        val isResending: Boolean = false,
        val resendSecondsLeft: Int = 0,
        val canUsePassword: Boolean = false,
        val isOpeningPassword: Boolean = false,
        val sentTo: String? = null,
    ) : ScreenPlan

    /** The password of [identifier], typed as [method]; [canSendCode] adds the way to a code. */
    data class Password(
        val identifier: String,
        val method: LoginOption = LoginOption.EmailOtp,
        val canSendCode: Boolean = false,
        val error: EmbeddedLoginError? = null,
        val isSubmitting: Boolean = false,
        val isSendingCode: Boolean = false,
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
        is ScreenPlan.Password -> "password"
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
        // A method's own screen is reached from the first one, so it sits one deeper.
        is ScreenPlan.Identifier -> if (isFirst) 1 else 2
        is ScreenPlan.OtpCode, is ScreenPlan.Password -> 3
        is ScreenPlan.Mfa -> 4
        ScreenPlan.Authenticated, is ScreenPlan.Failed -> 5
    }

internal fun plan(step: LoginState): ScreenPlan = when (step) {
    LoginState.Loading -> ScreenPlan.Loading
    is LoginState.CollectOtpIdentifier -> ScreenPlan.Identifier(
        value = step.value,
        method = step.method,
        otherMethods = step.otherMethods,
        isSubmitting = step.isSubmitting,
        country = step.country,
        error = step.error,
        isFirst = step.isFirst,
    )
    is LoginState.CollectOtpCode -> ScreenPlan.OtpCode(
        identifier = step.identifier,
        method = step.method,
        error = step.error,
        isSubmitting = step.isSubmitting,
        isResending = step.isResending,
        resendSecondsLeft = step.resendSecondsLeft,
        canUsePassword = step.canUsePassword,
        isOpeningPassword = step.isOpeningPassword,
        sentTo = step.sentTo,
    )
    is LoginState.CollectPassword -> ScreenPlan.Password(
        identifier = step.identifier,
        method = step.method,
        canSendCode = step.canSendCode,
        error = step.error,
        isSubmitting = step.isSubmitting,
        isSendingCode = step.isSendingCode,
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

        is ScreenPlan.Identifier -> {
            val error = plan.error?.text(plan.method)
            val choose = { other: LoginOption -> context.dispatch(LoginIntent.SwitchMethod(other)) }
            when (plan.method) {
                LoginOption.PhoneOtp -> LoginPhoneScreen(
                    phone = plan.value,
                    country = plan.country,
                    isLoading = plan.isSubmitting,
                    errorMessage = error,
                    otherMethods = plan.otherMethods,
                    onChooseMethod = choose,
                    onContinue = { number, country ->
                        context.dispatch(LoginIntent.SubmitIdentifier(plan.method, number, country))
                    },
                )

                LoginOption.Username -> LoginUsernameScreen(
                    username = plan.value,
                    isLoading = plan.isSubmitting,
                    errorMessage = error,
                    otherMethods = plan.otherMethods,
                    onChooseMethod = choose,
                    onContinue = { context.dispatch(LoginIntent.SubmitIdentifier(plan.method, it)) },
                )

                else -> LoginEmailScreen(
                    email = plan.value,
                    isLoading = plan.isSubmitting,
                    errorMessage = error,
                    otherMethods = plan.otherMethods,
                    onChooseMethod = choose,
                    onContinue = { context.dispatch(LoginIntent.SubmitIdentifier(plan.method, it)) },
                )
            }
        }

        is ScreenPlan.OtpCode -> LoginCodeScreen(
            // Where the server says the code went, as it hides it; else what the user typed, as it is.
            subtitle = stringResource(
                R.string.embedded_login_code_from,
                plan.sentTo?.forDisplay() ?: plan.identifier,
            ),
            subtitleFontFeatures = NO_CONTEXTUAL_ALTERNATES.takeIf { plan.sentTo != null },
            isLoading = plan.isSubmitting,
            isResending = plan.isResending,
            isOpeningPassword = plan.isOpeningPassword,
            resendSecondsLeft = plan.resendSecondsLeft,
            errorMessage = plan.error?.text(plan.method),
            needsNewCode = plan.error?.needsNewCode == true,
            onVerify = { context.dispatch(LoginIntent.SubmitOtp(it)) },
            onEdit = context.back,
            onResend = { context.dispatch(LoginIntent.Resend) },
            onUsePassword = if (plan.canUsePassword) {
                { context.dispatch(LoginIntent.UsePassword) }
            } else {
                null
            },
        )

        is ScreenPlan.Password -> LoginPasswordScreen(
            identifierLabel = stringResource(plan.method.fieldLabel),
            editDescription = stringResource(plan.method.editDescription),
            identifier = plan.identifier,
            isLoading = plan.isSubmitting,
            isSendingCode = plan.isSendingCode,
            errorMessage = plan.error?.text(plan.method),
            password = context.typedPassword.value,
            onPasswordChange = { context.typedPassword.value = it },
            onContinue = { context.dispatch(LoginIntent.SubmitPassword(it)) },
            onEdit = context.back,
            onSendCode = if (plan.canSendCode) {
                { context.dispatch(LoginIntent.SendCode) }
            } else {
                null
            },
            sendCodeTitle = stringResource(plan.method.sendCodeTitle),
            sendCodeIcon = plan.method.continueWithIcon,
            sendCodeSecondsLeft = plan.resendSecondsLeft,
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
    EmbeddedLoginError.IpBlocked -> stringResource(R.string.embedded_login_error_ip_blocked)
    EmbeddedLoginError.ConsentRequired -> stringResource(R.string.embedded_login_error_consent_required)
    EmbeddedLoginError.SessionExpired -> stringResource(R.string.embedded_login_error_session_expired)
    EmbeddedLoginError.CodeNoLongerValid -> stringResource(R.string.embedded_login_error_code_no_longer_valid)
    is EmbeddedLoginError.InvalidPassword -> stringResource(
        when (this.method) {
            LoginOption.PhoneOtp -> R.string.embedded_login_error_invalid_password_phone
            LoginOption.Username -> R.string.embedded_login_error_invalid_password_username
            else -> R.string.embedded_login_error_invalid_password_email
        },
    )
    EmbeddedLoginError.PasswordLeaked -> stringResource(R.string.embedded_login_error_password_leaked)
    EmbeddedLoginError.PasswordNotAvailable -> stringResource(R.string.embedded_login_error_password_not_available)
    EmbeddedLoginError.NoNextSteps -> stringResource(R.string.embedded_login_error_no_next_steps)
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

/** Only a new code helps: the one on the screen can't be checked any more, however much the user retypes it. */
internal val EmbeddedLoginError.needsNewCode: Boolean
    get() = this == EmbeddedLoginError.CodeExpired || this == EmbeddedLoginError.CodeNoLongerValid

/** The label over the field of this method, and over the row that shows what was typed in it on the screens after. */
internal val LoginOption.fieldLabel: Int
    get() = when (this) {
        LoginOption.PhoneOtp -> R.string.phone_number_label
        LoginOption.Username -> R.string.embedded_login_username_label
        else -> R.string.embedded_login_email_label
    }

/** What a screen reader says for Edit on that row: its own words for each method, not the label made lower case. */
internal val LoginOption.editDescription: Int
    get() = when (this) {
        LoginOption.PhoneOtp -> R.string.embedded_login_edit_phone
        LoginOption.Username -> R.string.embedded_login_edit_username
        else -> R.string.embedded_login_edit_email
    }

/** The password screen's way to a code, for an email or a number the server offers both for. */
internal val LoginOption.sendCodeTitle: Int
    get() = when (this) {
        LoginOption.EmailOtp -> R.string.embedded_login_send_code_email
        LoginOption.PhoneOtp -> R.string.embedded_login_send_code_phone
        else -> R.string.embedded_login_send_code
    }

/** The code and the password of the same person are two ways of finishing one step: moving between them is a swap. */
internal fun ScreenPlan.isSiblingOf(other: ScreenPlan): Boolean =
    (this is ScreenPlan.OtpCode && other is ScreenPlan.Password) ||
        (this is ScreenPlan.Password && other is ScreenPlan.OtpCode)
