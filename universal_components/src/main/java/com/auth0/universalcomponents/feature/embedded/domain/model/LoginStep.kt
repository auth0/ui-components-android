package com.auth0.universalcomponents.feature.embedded.domain.model

/** Where the login flow is. The ViewModel moves between steps; the renderer draws one screen per step. */
internal sealed interface LoginStep {

    data object Loading : LoginStep

    /** [email] pre-fills the field, so Edit and Try again keep what was typed. */
    data class CollectOtpIdentifier(val email: String = "") : LoginStep

    data class CollectOtpCode(val identifier: String) : LoginStep

    data class StepUpMfa(val challenge: MfaChallenge) : LoginStep

    data object Authenticated : LoginStep

    data class Failed(val message: FailureMessage) : LoginStep

    data object NoMethodsAvailable : LoginStep
}

/** What the screens ask the ViewModel to do. */
internal sealed interface LoginEvent {

    data object Start : LoginEvent

    data class SubmitIdentifier(val email: String) : LoginEvent

    data class SubmitOtp(val code: String) : LoginEvent

    data class SubmitMfa(val code: String) : LoginEvent

    data object Retry : LoginEvent
}
