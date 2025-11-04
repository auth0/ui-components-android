package com.auth0.android.ui_components.domain.model

/**
 * Wrapper for different enrollment challenge types
 */
sealed class EnrollmentResult {
    /**
     * TOTP or Push Notification enrollment result
     */
    data class TotpEnrollment(
        val challenge: TotpEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()

    /**
     * Recovery code enrollment result
     */
    data class RecoveryCodeEnrollment(
        val challenge: RecoveryCodeEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()

    /**
     * Email or Phone enrollment result
     * Generic enrollment challenge for OTP-based methods
     */
    data class DefaultEnrollment(
        val challenge: MfaEnrollmentChallenge,
        val authenticationMethodId: String,
        val authSession: String
    ) : EnrollmentResult()
}
