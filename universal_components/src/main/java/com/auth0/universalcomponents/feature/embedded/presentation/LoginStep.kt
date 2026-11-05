package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaEngine

/**
 * Where the login flow is, with everything its screen shows. The ViewModel moves between steps;
 * the renderer draws one screen per step.
 *
 * Every step is a plain value except [Mfa], which carries a live engine (a coroutine scope and mutable state). Two
 * [Mfa] steps are equal when they hold the same engine, and a [copy][Mfa.copy] points at the same running engine, not a
 * new one, so don't treat it as a value like the others.
 */
internal sealed interface LoginStep {

    data object Loading : LoginStep

    /**
     * [email] pre-fills the field, so Edit and Try again keep what was typed.
     * [isSubmitting] spins Continue while the code is being sent.
     */
    data class CollectOtpIdentifier(
        val email: String = "",
        val isSubmitting: Boolean = false,
    ) : LoginStep

    /**
     * [isSubmitting] spins Verify; [isResending] busies only the resend row while a new code is on its way.
     * [resendSecondsLeft] counts down from each code sent; Resend opens at zero.
     */
    data class CollectOtpCode(
        val identifier: String,
        val error: EmbeddedLoginError? = null,
        val isSubmitting: Boolean = false,
        val isResending: Boolean = false,
        val resendSecondsLeft: Int = 0,
    ) : LoginStep

    /**
     * The first factor passed and the server wants a second; MFA carries on with the same session. It draws itself
     * from [engine] and takes its own taps, and login hears only how it ends. [identifier] is what the user typed for
     * the first factor, to pre-fill it if they start over.
     */
    data class Mfa(val engine: MfaEngine, val identifier: String) : LoginStep

    data object Authenticated : LoginStep

    /** Try again goes to [retryTo]; when null, it opens a new session. */
    data class Failed(val error: EmbeddedLoginError, val retryTo: LoginStep? = null) : LoginStep

    data object NoMethodsAvailable : LoginStep
}

/** What the screens ask the ViewModel to do. */
internal sealed interface LoginEvent {

    data object Start : LoginEvent

    data class SubmitIdentifier(val email: String) : LoginEvent

    data class SubmitOtp(val code: String) : LoginEvent

    data object Resend : LoginEvent

    data object Retry : LoginEvent

    /** "Start over" on MFA's error screen: back to email entry. Counts only while MFA is on show. */
    data object LeaveMfa : LoginEvent
}
