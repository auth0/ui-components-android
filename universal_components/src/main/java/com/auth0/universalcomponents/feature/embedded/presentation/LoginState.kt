package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.android.result.Credentials
import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.domain.model.LoginOption
import com.auth0.universalcomponents.feature.mfachallenge.MfaError
import com.auth0.universalcomponents.feature.mfachallenge.MfaErrorHandling
import com.auth0.universalcomponents.feature.mfachallenge.presentation.MfaEngine

/**
 * Where the login flow is, with everything its screen shows. The ViewModel moves between steps;
 * the renderer draws one screen per step.
 *
 * Every step is a plain value except [Mfa] and [AwaitingApp], which carry a live engine (a coroutine scope and mutable
 * state). Two of them are equal when they hold the same engine, and a copy points at the same running engine, not a
 * new one, so don't treat them as values like the others.
 */
internal sealed interface LoginState {

    data object Loading : LoginState

    /**
     * The screen asking for the email or the phone number a code goes to, as [method] says. [value] pre-fills the field
     * (a phone number in E.164 form), so Edit and Try again keep what was typed. [otherMethod] is the one "Continue
     * with" switches to; null hides it. [isSubmitting] spins Continue while the code is being sent.
     */
    data class CollectOtpIdentifier(
        val method: LoginOption = LoginOption.EmailOtp,
        val value: String = "",
        val otherMethod: LoginOption? = null,
        val isSubmitting: Boolean = false,
    ) : LoginState

    /**
     * [isSubmitting] spins Verify; [isResending] busies only the resend row while a new code is on its way.
     * [resendSecondsLeft] counts down from each code sent; Resend opens at zero.
     */
    data class CollectOtpCode(
        val identifier: String,
        val method: LoginOption = LoginOption.EmailOtp,
        val error: EmbeddedLoginError? = null,
        val isSubmitting: Boolean = false,
        val isResending: Boolean = false,
        val resendSecondsLeft: Int = 0,
    ) : LoginState

    /**
     * The first factor passed and the server wants a second; MFA carries on with the same session. It draws itself
     * from [engine] and takes its own taps, and login hears only how it ends. [identifier] is what the user typed for
     * the first factor with [method], to pre-fill it if they start over.
     */
    data class Mfa(val engine: MfaEngine, val identifier: String, val method: LoginOption) : LoginState

    /**
     * MFA ended with no factor this app can use, and login waits for the app to say whether it takes over (a spinner
     * shows meanwhile, so an app that does never sees an error flash by). If it doesn't, the [engine]'s own
     * "not available" screen comes back. It is not an MFA screen and not a login error, so it is its own step.
     */
    data class AwaitingApp(val engine: MfaEngine, val identifier: String, val method: LoginOption) : LoginState

    data object Authenticated : LoginState

    /** Try again goes to [retryTo]; when null, it opens a new session. */
    data class Failed(val error: EmbeddedLoginError, val retryTo: LoginState? = null) : LoginState

    data object NoMethodsAvailable : LoginState
}

/** What the screens ask the ViewModel to do. */
internal sealed interface LoginIntent {

    data object Start : LoginIntent

    /** [method] is the screen it was typed on, so a tap that outlives a switch to the other method is dropped. */
    data class SubmitIdentifier(val method: LoginOption, val value: String) : LoginIntent

    /** "Continue with …" on the first screen. Counts only there, and not while a code is being sent. */
    data class SwitchMethod(val method: LoginOption) : LoginIntent

    data class SubmitOtp(val code: String) : LoginIntent

    data object Resend : LoginIntent

    data object Retry : LoginIntent

    /** "Start over" on MFA's error screen: back to the first factor's entry. Counts only while MFA is on show. */
    data object LeaveMfa : LoginIntent

    /** The app's answer to an MFA error it was told about. Counts only while login waits for it. */
    data class MfaErrorAnswered(val handling: MfaErrorHandling) : LoginIntent
}

/**
 * One-time messages from the ViewModel to the screen: each is handled once and never kept, so a rotation cannot
 * repeat it. What the screen draws is [LoginState]; what the user sends in is [LoginIntent].
 */
internal sealed interface LoginEvent {

    /** The user is signed in; the credentials go to the app's `onAuthenticated`. */
    data class Authenticated(val credentials: Credentials) : LoginEvent

    /** MFA ended with an error, for the app's `onMfaError`. The user leaving MFA is not one. */
    data class MfaFailed(val error: MfaError) : LoginEvent
}
