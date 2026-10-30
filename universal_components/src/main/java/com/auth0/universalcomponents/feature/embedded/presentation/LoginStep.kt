package com.auth0.universalcomponents.feature.embedded.presentation

import com.auth0.universalcomponents.feature.embedded.domain.model.EmbeddedLoginError
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaIntent
import com.auth0.universalcomponents.feature.embedded.presentation.mfa.MfaStep

/**
 * Where the login flow is, with everything its screen shows. The ViewModel moves between steps;
 * the renderer draws one screen per step.
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
     * The code was right and the server wants a second factor; MFA carries on with the same session and [step] is
     * its screen. [email] is kept for when MFA sends the user back.
     */
    data class Mfa(val email: String, val step: MfaStep) : LoginStep

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

    /** A tap on an MFA screen, passed on to the MFA flow. */
    data class Mfa(val intent: MfaIntent) : LoginEvent
}
